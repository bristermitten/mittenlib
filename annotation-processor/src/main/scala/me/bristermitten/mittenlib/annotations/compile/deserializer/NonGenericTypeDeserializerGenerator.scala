package me.bristermitten.mittenlib.annotations.compile.deserializer

import com.google.gson.reflect.TypeToken
import com.google.inject.Inject
import com.palantir.javapoet.{
  ClassName,
  CodeBlock,
  MethodSpec,
  TypeName,
  ParameterizedTypeName
}
import io.toolisticon.aptk.tools.TypeMirrorWrapper
import java.util.Optional
import javax.lang.model.element.{Modifier, TypeElement}
import javax.lang.model.`type`.{DeclaredType, TypeMirror}
import _root_.me.bristermitten.mittenlib.annotations.ast.{
  AbstractConfigStructure,
  CustomDeserializerInfo,
  Property
}
import _root_.me.bristermitten.mittenlib.annotations.ast.ConfigTypeSource
import _root_.me.bristermitten.mittenlib.annotations.compile.ConfigurationClassNameGenerator
import _root_.me.bristermitten.mittenlib.annotations.parser.CustomDeserializers
import _root_.me.bristermitten.mittenlib.annotations.util.{
  NewtypeUtil,
  TypesUtil,
  ConfigStructureAnalysis
}
import _root_.me.bristermitten.mittenlib.config.exception.ConfigLoadingErrors
import _root_.me.bristermitten.mittenlib.config.extension.UseObjectMapperSerialization
import _root_.me.bristermitten.mittenlib.config.tree.{
  DataTree,
  DataTreeTransforms
}
import _root_.me.bristermitten.mittenlib.config.EnumParsingSchemes
import _root_.me.bristermitten.mittenlib.util.{Enums, Result, Strings}

import _root_.me.bristermitten.mittenlib.codegen.dsl.*
import _root_.me.bristermitten.mittenlib.codegen.dsl.BlockBuilder.*
import _root_.me.bristermitten.mittenlib.codegen.dsl.ResultExpr
import _root_.me.bristermitten.mittenlib.codegen.dsl.given
import _root_.me.bristermitten.mittenlib.config.DeserializationContext

class NonGenericTypeDeserializerGenerator @Inject() (
    private val typesUtil: TypesUtil,
    private val configurationClassNameGenerator: ConfigurationClassNameGenerator,
    private val customDeserializers: CustomDeserializers,
    private val configStructureAnalysis: ConfigStructureAnalysis
):

  private def getDeserializationFunction(
      context: StagedExpr[DeserializationContext],
      info: CustomDeserializerInfo,
      withDataExpression: Expr[?]
  ): Expr[?] =
    if (info.isStatic) {
      Expr.staticCall(
        TypeRef.of(ClassName.get(info.deserializerClass)),
        "deserialize",
        context.withData(withDataExpression)
      )
    } else {
      val fieldName =
        Strings.uncapitalize(info.deserializerClass.getSimpleName.toString)
      val provider = StagedExpr
        .field[java.util.function.Function[DeserializationContext, Result[?]]](
          Types
            .Function(Types.DeserializationContext, Types.Result(Types.Object)),
          fieldName
        )
      provider(context.withData(withDataExpression))
    }

  def dataTreeConvert(
      tpe: TypeName,
      dataTreeType: TypeName,
      value: Expr[?]
  ): Expr[?] =
    val targetType = if (tpe.isBoxedPrimitive) tpe.unbox() else tpe
    if (
      dataTreeType == ClassName.get(
        classOf[DataTree.DataTreeLiteral.DataTreeLiteralInt]
      )
    ) {
      if (targetType == TypeName.INT) return value.call("intValue")
      if (targetType == TypeName.SHORT) return value.call("shortValue")
      if (targetType == TypeName.BYTE) return value.call("byteValue")
      if (targetType == TypeName.LONG) return value.call("longValue")
    }
    if (
      dataTreeType == ClassName.get(
        classOf[DataTree.DataTreeLiteral.DataTreeLiteralFloat]
      )
    ) {
      if (targetType == TypeName.FLOAT) return value.call("floatValue")
      if (targetType == TypeName.DOUBLE) return value.call("doubleValue")
    }
    value

  private def getLiteralValueType(safeType: TypeName): Class[?] =
    val unboxed = if (safeType.isBoxedPrimitive) safeType.unbox() else safeType
    if (unboxed == TypeName.BOOLEAN) classOf[java.lang.Boolean]
    else if (unboxed == ClassName.get(classOf[String])) classOf[String]
    else classOf[java.lang.Number]

  def handleNonGenericType(
      property: Property,
      dtoType: TypeElement,
      elementType: TypeMirror,
      wrappedElementType: TypeMirrorWrapper,
      fromMap: Expr[?],
      context: StagedExpr[DeserializationContext],
      safeType: TypeName
  )(using BlockBuilder): Block[Terminated] =
    val fm = ~fromMap
    val safeTypeRef = TypeRef.of(safeType)
    val instanceOfTypeRef = safeType match {
      case p: ParameterizedTypeName => TypeRef.of(p.rawType)
      case _                        => safeTypeRef
    }

    // 3.1 Direct Type Match
    if (!fromMap.isInstanceOf[Expr.MethodCall]) {
      if (
        property.settings().hasDefaultValue() || configStructureAnalysis
          .isTypeInitializable(elementType)
      ) {
        ifThen(fm.instanceOf(instanceOfTypeRef)) {
          return_(ResultExpr.ok(fm.cast(safeTypeRef)))
        }
      }
    }

    // 3.2 DataTree Type Match
    val treeTypeOpt = typesUtil.getDataTreeType(safeType)
    if (treeTypeOpt.isDefined) {
      val treeType = TypeRef.of(treeTypeOpt.get)
      ifThen(fm.instanceOf(treeType)) {
        val convert = dataTreeConvert(
          safeType,
          treeTypeOpt.get,
          (~fm.cast(treeType))
            .as[DataTree]
            .value
            .cast(TypeRef.of(getLiteralValueType(safeType)))
        )
        return_(ResultExpr.ok(convert))
      }
    }

    // 3.3 Custom Deserializers (no fallback)
    val customDeserializerOptional =
      customDeserializers.getCustomInfo(property.propertyType())

    if (
      customDeserializerOptional.isPresent && !customDeserializerOptional
        .get()
        .isFallback
    ) {
      return_(
        getDeserializationFunction(
          context,
          customDeserializerOptional.get(),
          DataTreeExpr.loadFrom(fm)
        )
      )
    } else if (wrappedElementType.isEnum) {
      if (property.settings().hasDefaultValue()) {
        ifThen(fm.instanceOf(Types.String)) {
          addEnumDeserialisation(property, fm, safeType, fm)
        }
      }
      val stringLitType =
        TypeRef.of(classOf[DataTree.DataTreeLiteral.DataTreeLiteralString])
      ifThen(fm.instanceOf(stringLitType)) {
        val convert = (~fm.cast(stringLitType)).as[DataTree].value.cast[String]
        addEnumDeserialisation(property, fm, safeType, convert)
      }
      val typeTokenT = TypeRef.of(classOf[TypeToken[?]])(safeTypeRef)
      return_(context.getMapper.map(fm, Expr.newAnonymous(typeTokenT)))
    } else if (typesUtil.isConfigType(elementType)) {
      val loaderFieldName =
        configurationClassNameGenerator.getDeserializerProviderFieldName(
          elementType
        )
      val mapDataType = TypeRef.of(classOf[DataTree.DataTreeMap])
      ifThen(fm.instanceOf(mapDataType)) {
        val mapData = declare(mapDataType, "mapData", fm.cast(mapDataType))
        val provider = StagedExpr.field[com.google.inject.Provider[
          java.util.function.Function[DeserializationContext, Result[?]]
        ]](
          Types.Provider(
            Types.Function(
              Types.DeserializationContext,
              Types.Result(Types.Object)
            )
          ),
          loaderFieldName
        )
        return_(provider.get.apply(context.withData(mapData)))
      }
      val typeTokenT = TypeRef.of(classOf[TypeToken[?]])(safeTypeRef)
      return_(context.getMapper.map(fm, Expr.newAnonymous(typeTokenT)))
    } else if (typesUtil.isNewtype(elementType)) {
      val element = elementType
        .asInstanceOf[DeclaredType]
        .asElement()
        .asInstanceOf[TypeElement]
      val underlying = typesUtil.getNewtypeUnderlyingType(elementType)
      val boxedUnderlying = typesUtil.getBoxedType(underlying)
      val typeName = TypeName.get(elementType)
      val implClassBase = NewtypeUtil.getImplClassName(element)
      val implClass = typeName match {
        case p: ParameterizedTypeName =>
          ParameterizedTypeName.get(
            implClassBase,
            p.typeArguments.toArray(new Array[TypeName](0))*
          )
        case _ => implClassBase
      }
      val typeTokenT = TypeRef.of(classOf[TypeToken[?]])(
        TypeRef.of(TypeName.get(boxedUnderlying))
      )
      val lambdaParam = Var("val", TypeRef.of(TypeName.get(boxedUnderlying)))

      return_(
        context.getMapper
          .map(fm, Expr.newAnonymous(typeTokenT))
          .mapResult(
            Expr.lambdaExpr(
              Expr.new_(TypeRef.of(implClass), lambdaParam),
              lambdaParam
            )
          )
      )
    } else if (
      customDeserializerOptional.isPresent && customDeserializerOptional
        .get()
        .isFallback
    ) {
      return_(
        getDeserializationFunction(
          context,
          customDeserializerOptional.get(),
          DataTreeExpr.loadFrom(fm)
        )
      )
    } else {
      val useObjectMapperSerialization = typesUtil.getAnnotation(
        property.source().element(),
        classOf[UseObjectMapperSerialization]
      )
      if (useObjectMapperSerialization != null) {
        val propertyTypeName = TypeRef.of(
          configurationClassNameGenerator.publicPropertyClassName(property)
        )
        val dataTreeTransforms = TypeRef.of(classOf[DataTreeTransforms])
        return_(
          context.getMapper.map(
            Expr.staticCall(
              dataTreeTransforms,
              "toPOJO",
              DataTreeExpr.loadFrom(fm)
            ),
            Expr.staticCall(
              Types.TypeToken,
              "get",
              Expr.staticField(propertyTypeName, "class")
            )
          )
        )
      } else {
        if (property.settings().hasDefaultValue()) {
          val dataTreeType = Types.DataTree
          ifThen(!fm.instanceOf(dataTreeType)) {
            return_(
              ResultExpr.fail(
                Expr.staticCall(
                  TypeRef.of(classOf[ConfigLoadingErrors]),
                  "invalidPropertyTypeException",
                  Expr.staticField(TypeRef.of(ClassName.get(dtoType)), "class"),
                  Expr.str(property.name()),
                  Expr.str(elementType.toString),
                  fm
                )
              )
            )
          }
        }

        val typeTokenT = TypeRef.of(classOf[TypeToken[?]])(safeTypeRef)
        return_(context.getMapper.map(fm, Expr.newAnonymous(typeTokenT)))
      }
    }

  def generateDeserializeMethod(
      propertyAST: AbstractConfigStructure,
      property: Property,
      dtoType: TypeElement,
      elementType: TypeMirror,
      wrappedElementType: TypeMirrorWrapper,
      safeType: TypeName,
      daoName: ClassName,
      fieldNameGenerator: _root_.me.bristermitten.mittenlib.annotations.compile.FieldNameGenerator,
      methodNames: _root_.me.bristermitten.mittenlib.annotations.compile.MethodNames
  ): MethodSpec =
    val elementResultType = TypeRef.of(safeType)
    val context = StagedExpr
      .param[DeserializationContext](Types.DeserializationContext, "context")

    val hasDefault =
      configStructureAnalysis.hasDefaultOrIsInitializable(property)
    val dao = if (hasDefault && daoName != null) {
      Some(StagedExpr.param[Any](TypeRef.of(daoName), "dao"))
    } else {
      None
    }

    val params = List(context.asVar) ++ dao.map(_.asVar)

    val methodDecl = MethodDecl.build(
      name = methodNames.getDeserializeMethodName(property),
      returnType = Types.Result(elementResultType),
      parameters = params,
      modifiers = List(Modifier.PRIVATE)
    ) {
      // 1. Initial statements and Null checks
      val fromMap =
        _root_.me.bristermitten.mittenlib.annotations.compile.GeneratorUtil
          .declareAndCheckFromMap(
            propertyAST,
            property,
            dtoType,
            elementType,
            context,
            hasDefault,
            dao,
            fieldNameGenerator
          )

      // 3. Handlers
      handleNonGenericType(
        property,
        dtoType,
        elementType,
        wrappedElementType,
        fromMap,
        context,
        safeType
      )
    }

    CodeBlockRenderer.renderMethod(methodDecl)

  private def addEnumDeserialisation(
      property: Property,
      fromMap: Expr[?],
      safeType: TypeName,
      convert: Expr[?]
  )(using BlockBuilder): Block[Terminated] =
    val safeTypeRef = TypeRef.of(safeType)
    val fm = ~fromMap
    val cv = ~convert
    val enumValue = property.settings().enumParsingScheme() match
      case EnumParsingSchemes.EXACT_MATCH =>
        declare(
          safeTypeRef,
          "enumValue",
          Expr.staticCall(
            TypeRef.of(classOf[Enums]),
            "valueOfOrNull",
            cv.cast(Types.String),
            Expr.staticField(safeTypeRef, "class")
          )
        )
      case EnumParsingSchemes.CASE_INSENSITIVE =>
        declare(
          safeTypeRef,
          "enumValue",
          Expr.staticCall(
            TypeRef.of(classOf[Enums]),
            "valueOfIgnoreCase",
            cv.cast(Types.String),
            Expr.staticField(safeTypeRef, "class")
          )
        )

    val ev = ~enumValue
    ifThen(ev.isNull) {
      return_(
        ResultExpr.fail(
          Expr.staticCall(
            TypeRef.of(classOf[ConfigLoadingErrors]),
            "invalidEnumException",
            Expr.staticField(safeTypeRef, "class"),
            Expr.str(property.name()),
            fm
          )
        )
      )
    }

    return_(ResultExpr.ok(ev))
