package me.bristermitten.mittenlib.annotations.compile.deserializer

import com.google.gson.reflect.TypeToken
import com.google.inject.Inject
import com.palantir.javapoet.{ClassName, CodeBlock, MethodSpec, TypeName, ParameterizedTypeName}
import io.toolisticon.aptk.tools.TypeMirrorWrapper
import io.toolisticon.aptk.tools.corematcher.AptkCoreMatchers
import io.toolisticon.aptk.tools.wrapper.{ElementWrapper, TypeElementWrapper}
import java.util.Optional
import javax.lang.model.element.{Modifier, TypeElement}
import javax.lang.model.`type`.TypeMirror
import _root_.me.bristermitten.mittenlib.annotations.ast.{AbstractConfigStructure, ConfigTypeSource, CustomDeserializerInfo, Property}
import _root_.me.bristermitten.mittenlib.annotations.compile.ConfigurationClassNameGenerator
import _root_.me.bristermitten.mittenlib.annotations.parser.CustomDeserializers
import _root_.me.bristermitten.mittenlib.annotations.util.TypesUtil
import _root_.me.bristermitten.mittenlib.config.CollectionsUtils
import _root_.me.bristermitten.mittenlib.config.exception.ConfigLoadingErrors
import _root_.me.bristermitten.mittenlib.util.Strings

import _root_.me.bristermitten.mittenlib.codegen.dsl.*
import _root_.me.bristermitten.mittenlib.codegen.dsl.BlockBuilder.*
import _root_.me.bristermitten.mittenlib.codegen.dsl.ResultExpr
import _root_.me.bristermitten.mittenlib.codegen.dsl.given
import _root_.me.bristermitten.mittenlib.config.DeserializationContext

class GenericTypeDeserializerGenerator @Inject() (
  private val typesUtil: TypesUtil,
  private val configurationClassNameGenerator: ConfigurationClassNameGenerator,
  private val customDeserializers: CustomDeserializers,
  private val nonGenericTypeDeserializerGenerator: NonGenericTypeDeserializerGenerator
):

  private def getDeserializationFunction(
    dtoType: TypeElement,
    property: Property,
    tpe: TypeMirror,
    depth: Int
  ): Expr =
    val wrapped = TypeMirrorWrapper.wrap(tpe)
    val customDeserializerOptional = customDeserializers.getCustomInfo(tpe)
    if (customDeserializerOptional.isPresent) {
      val info = customDeserializerOptional.get()
      if (info.isStatic) {
        val ctxVar = Var("ctx", Types.DeserializationContext)
        Expr.lambdaExpr(
          Expr.staticCall(TypeRef.of(ClassName.get(info.deserializerClass)), "deserialize", ctxVar),
          ctxVar
        )
      } else {
        val fieldName = Strings.uncapitalize(info.deserializerClass.getSimpleName.toString)
        Expr.This.field(fieldName)
      }
    } else if (typesUtil.isConfigType(tpe)) {
      val loaderField = configurationClassNameGenerator.getDeserializerProviderFieldName(tpe)
      Expr.This.field(loaderField).call("get")
    } else if (wrapped.hasTypeArguments && !typesUtil.isNewtype(tpe)) {
      val canonicalName = wrapped.erasure().getQualifiedName()
      val ctxVar = Var(s"ctx$depth", Types.DeserializationContext)
      if (canonicalName == classOf[java.util.List[?]].getName) {
        val elementType = wrapped.getTypeArguments.get(0)
        val innerFunction = getDeserializationFunction(dtoType, property, elementType, depth + 1)
        Expr.lambdaExpr(
          Expr.staticCall(TypeRef.of(classOf[CollectionsUtils]), "deserializeList",
            ctxVar.call("getData"), ctxVar, innerFunction
          ),
          ctxVar
        )
      } else if (canonicalName == classOf[java.util.Set[?]].getName) {
        val elementType = wrapped.getTypeArguments.get(0)
        val innerFunction = getDeserializationFunction(dtoType, property, elementType, depth + 1)
        Expr.lambdaExpr(
          Expr.staticCall(TypeRef.of(classOf[CollectionsUtils]), "deserializeSet",
            ctxVar.call("getData"), ctxVar, innerFunction
          ),
          ctxVar
        )
      } else if (canonicalName == classOf[java.util.Map[?, ?]].getName) {
        val keyType = wrapped.getTypeArguments.get(0)
        val valueType = wrapped.getTypeArguments.get(1)
        val innerFunction = getDeserializationFunction(dtoType, property, valueType, depth + 1)
        Expr.lambdaExpr(
          Expr.staticCall(TypeRef.of(classOf[CollectionsUtils]), "deserializeMap",
            Expr.staticField(TypeRef.of(TypeName.get(typesUtil.getSafeType(keyType))), "class"),
            ctxVar.call("getData"), ctxVar, innerFunction
          ),
          ctxVar
        )
      } else {
        throw new IllegalStateException("Unexpected nested generic type: " + canonicalName)
      }
    } else {
      val ctxVar = Var(s"ctx$depth", Types.DeserializationContext)
      val safeType = configurationClassNameGenerator.publicPropertyClassName(typesUtil.getBoxedType(tpe))
      val lambdaBody = BlockBuilder.build {
        nonGenericTypeDeserializerGenerator.handleNonGenericType(
          property, dtoType, tpe, wrapped, ctxVar.call("getData"), (~ctxVar).as[DeserializationContext], safeType
        )
      }
      Expr.Lambda(List(ctxVar), lambdaBody)
    }

  def generateDeserializeMethod(
    propertyAST: AbstractConfigStructure,
    property: Property,
    dtoType: TypeElement,
    elementType: TypeMirror,
    wrappedElementType: TypeMirrorWrapper,
    elementTypeElement: TypeElementWrapper,
    safeType: TypeName,
    daoName: ClassName,
    fieldNameGenerator: _root_.me.bristermitten.mittenlib.annotations.compile.FieldNameGenerator,
    methodNames: _root_.me.bristermitten.mittenlib.annotations.compile.MethodNames
  ): MethodSpec =
    ElementWrapper.wrap(property.source().element())
      .validate()
      .asError()
      .check(el => AptkCoreMatchers.BY_RAW_TYPE
        .getValidator()
        .hasOneOf(elementTypeElement.unwrap(), classOf[java.util.List[?]], classOf[java.util.Set[?]], classOf[java.util.Map[?, ?]]))
      .validateAndIssueMessages()

    val elementResultType = TypeRef.of(safeType)
    val context = StagedExpr.param[DeserializationContext](Types.DeserializationContext, "context")
    
    val hasDefault = property.settings().hasDefaultValue()
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
      // 1. Initial statements
      val data = declare(Types.DataTree, "$data", context.getData)
      val key = fieldNameGenerator.getConfigFieldName(property)
      val fromMapName = property.name() + "FromMap"
      
      val fromMap = if (hasDefault) {
        val defaultAccess = propertyAST.source() match {
          case source: ConfigTypeSource.ClassConfigTypeSource => dao.get.field(property.name())
          case source: ConfigTypeSource.InterfaceConfigTypeSource => dao.get.call(property.name())
        }
        declare(Types.Object, fromMapName, data.call("getOrDefault", Expr.str(key), defaultAccess))
      } else {
        declare(Types.DataTree, fromMapName, data.call("get", Expr.str(key)))
      }
      
      // 2. Null checks
      if (property.settings().isNullable()) {
        ifThen(fromMap.isNull) {
          return_(ResultExpr.ok(Expr.Null))
        }
      } else {
        ifThen(fromMap.isNull) {
          return_(
            ResultExpr.fail(
              Expr.staticCall(TypeRef.of(classOf[ConfigLoadingErrors]), "notFoundException",
                Expr.str(property.name()),
                Expr.str(TypeName.get(elementType).withoutAnnotations().toString),
                Expr.staticField(TypeRef.of(dtoType), "class"),
                Expr.str(key)
              )
            )
          )
        }
      }
      
      // 3. Generic handling
      val canonicalName = wrappedElementType.erasure().getQualifiedName()
      
      if (canonicalName == classOf[java.util.List[?]].getName) {
        val listType = wrappedElementType.getTypeArguments().get(0)
        val deserializationFunction = getDeserializationFunction(dtoType, property, listType, 0)
        return_(
          Expr.staticCall(TypeRef.of(classOf[CollectionsUtils]), "deserializeList",
            fromMap, context, deserializationFunction
          )
        )
      } else if (canonicalName == classOf[java.util.Set[?]].getName) {
        val setType = wrappedElementType.getTypeArguments().get(0)
        val deserializationFunction = getDeserializationFunction(dtoType, property, setType, 0)
        return_(
          Expr.staticCall(TypeRef.of(classOf[CollectionsUtils]), "deserializeSet",
            fromMap, context, deserializationFunction
          )
        )
      } else if (canonicalName == classOf[java.util.Map[?, ?]].getName) {
        val keyType = wrappedElementType.getTypeArguments().get(0)
        val valueType = wrappedElementType.getTypeArguments().get(1)
        val deserializationFunction = getDeserializationFunction(dtoType, property, valueType, 0)
        return_(
          Expr.staticCall(TypeRef.of(classOf[CollectionsUtils]), "deserializeMap",
            Expr.staticField(TypeRef.of(TypeName.get(typesUtil.getSafeType(keyType))), "class"),
            fromMap, context, deserializationFunction
          )
        )
      } else {
        throw new IllegalStateException("Unexpected generic type: " + canonicalName)
      }
    }
    
    CodeBlockRenderer.renderMethod(methodDecl)
