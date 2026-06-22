package me.bristermitten.mittenlib.annotations.compile

import com.google.inject.Inject
import com.palantir.javapoet.*
import io.toolisticon.aptk.tools.TypeMirrorWrapper
import java.util.{ArrayList, Optional}
import javax.lang.model.element.{Modifier, TypeElement}
import javax.lang.model.`type`.{DeclaredType, TypeMirror}
import me.bristermitten.mittenlib.annotations.ast.{
  AbstractConfigStructure,
  CustomSerializerInfo,
  Property
}
import me.bristermitten.mittenlib.annotations.parser.CustomSerializers
import me.bristermitten.mittenlib.annotations.util.{NewtypeUtil, TypesUtil}
import me.bristermitten.mittenlib.config.{
  CollectionsUtils,
  SerializationContext
}
import me.bristermitten.mittenlib.config.extension.UseObjectMapperSerialization
import me.bristermitten.mittenlib.config.tree.{DataTree, DataTreeTransforms}
import me.bristermitten.mittenlib.util.Strings
import scala.jdk.CollectionConverters.*

import me.bristermitten.mittenlib.annotations.domain.{
  ConfigStructure => DomainConfigStructure
}

import _root_.me.bristermitten.mittenlib.codegen.dsl.*
import _root_.me.bristermitten.mittenlib.codegen.dsl.BlockBuilder.*
import _root_.me.bristermitten.mittenlib.codegen.dsl.given

class SerializationCodeGenerator @Inject() (
    private val configurationClassNameGenerator: ConfigurationClassNameGenerator,
    private val typesUtil: TypesUtil,
    private val customSerializers: CustomSerializers,
    private val methodNames: MethodNames
):

  def isSerializationSupported(ast: AbstractConfigStructure): Boolean =
    ast
      .properties()
      .asScala
      .forall(property => !propertyIsUnserializable(property))

  def isSerializationSupported(ast: DomainConfigStructure): Boolean =
    ast.properties.forall(property => !domainPropertyIsUnserializable(property))

  def getUnsupportedSerializationProperties(
      ast: AbstractConfigStructure
  ): java.util.List[String] =
    val unsupported = new ArrayList[String]()
    for (property <- ast.properties().asScala) {
      if (propertyIsUnserializable(property)) {
        unsupported.add(property.name() + " (" + property.propertyType() + ")")
      }
    }
    unsupported

  def getUnsupportedSerializationProperties(
      ast: DomainConfigStructure
  ): java.util.List[String] =
    val unsupported = new ArrayList[String]()
    for (property <- ast.properties) {
      if (domainPropertyIsUnserializable(property)) {
        unsupported.add(property.name + " (" + property.typeMirror + ")")
      }
    }
    unsupported

  private def domainPropertyIsUnserializable(
      property: me.bristermitten.mittenlib.annotations.domain.Property
  ): Boolean =
    val propertyTypeMirror = property.typeMirror
    if (
      typesUtil.getAnnotation(
        property.element,
        classOf[UseObjectMapperSerialization]
      ) != null
    ) {
      return false
    }
    val wrappedType = TypeMirrorWrapper.wrap(propertyTypeMirror)
    if (typesUtil.isNewtype(propertyTypeMirror)) {
      return false // domain properties don't carry underlying type info easily; treat as serializable
    }
    if (wrappedType.hasTypeArguments) {
      val canonicalName = wrappedType.erasure().getQualifiedName()
      if (
        typesUtil.isCollection(propertyTypeMirror) || canonicalName == classOf[
          java.util.Map[?, ?]
        ].getName
      ) {
        return false // collections are serializable at domain level
      }
      return true
    }
    if (typesUtil.isConfigType(propertyTypeMirror)) {
      return false
    }
    if (isKnownSerializableType(wrappedType)) {
      return false
    }
    customSerializers.getCustomInfo(propertyTypeMirror).isEmpty

  private def propertyIsUnserializable(property: Property): Boolean =
    val propertyTypeMirror = property.propertyType()
    if (
      typesUtil.getAnnotation(
        property.source().element(),
        classOf[UseObjectMapperSerialization]
      ) != null
    ) {
      return false
    }

    val wrappedType = TypeMirrorWrapper.wrap(propertyTypeMirror)

    // Newtypes are serializable if their underlying type is serializable
    if (typesUtil.isNewtype(propertyTypeMirror)) {
      val underlyingType =
        typesUtil.getNewtypeUnderlyingType(propertyTypeMirror)
      return propertyIsUnserializable(
        new Property(
          property.name(),
          underlyingType,
          property.source(),
          property.settings()
        )
      )
    }

    // Check generic types
    if (wrappedType.hasTypeArguments) {
      val canonicalName = wrappedType.erasure().getQualifiedName()
      if (
        typesUtil.isCollection(propertyTypeMirror) || canonicalName == classOf[
          java.util.Map[?, ?]
        ].getName
      ) {
        val typeArguments = wrappedType.getTypeArguments.asScala
        for (typeArgument <- typeArguments) {
          if (
            propertyIsUnserializable(
              new Property(
                property.name(),
                typeArgument,
                property.source(),
                property.settings()
              )
            )
          ) {
            return true
          }
        }
        return false
      }
      return true
    }

    if (typesUtil.isConfigType(propertyTypeMirror)) {
      return false
    }

    if (isKnownSerializableType(wrappedType)) {
      return false
    }

    customSerializers.getCustomInfo(propertyTypeMirror).isEmpty

  def addSerializeMethodsToSaver(
      typeSpecBuilder: TypeSpec.Builder,
      ast: AbstractConfigStructure
  ): Unit =
    for (property <- ast.properties().asScala) {
      val serializeMethod = createSerializeMethodFor(property)
      typeSpecBuilder.addMethod(serializeMethod)
    }

  private def getSerializeParameterType(property: Property): TypeName =
    val typeName =
      configurationClassNameGenerator.publicPropertyClassName(property)
    typeName match
      case parameterizedTypeName: ParameterizedTypeName =>
        val rawType = parameterizedTypeName.rawType()
        if (
          rawType == ClassName.get(classOf[java.util.List[?]]) ||
          rawType == ClassName.get(classOf[java.util.Set[?]]) ||
          rawType == ClassName.get(classOf[java.util.Map[?, ?]])
        ) {
          val typeArguments = parameterizedTypeName
            .typeArguments()
            .asScala
            .map { arg =>
              WildcardTypeName.subtypeOf(arg).asInstanceOf[TypeName]
            }
            .asJava
          ParameterizedTypeName.get(rawType, typeArguments.asScala.toArray*)
        } else {
          typeName
        }
      case _ => typeName

  private def createSerializeMethodFor(property: Property): MethodSpec =
    val methodName = methodNames.getSerializeMethodName(property)
    val propertyType = getSerializeParameterType(property)
    val useObjectMapper = typesUtil.getAnnotation(
      property.source().element(),
      classOf[UseObjectMapperSerialization]
    ) != null

    val builder = MethodSpec
      .methodBuilder(methodName)
      .addJavadoc(
        """Serializes the {@code $L} property into a {@link $T}.
          |
          |@param value the value to serialize
          |@param context the serialization context
          |@return the serialized DataTree representation
          |""".stripMargin,
        property.name(),
        classOf[DataTree]
      )
      .addModifiers(Modifier.PRIVATE)
      .returns(classOf[DataTree])
      .addParameter(propertyType, "value")
      .addParameter(classOf[SerializationContext], "context")

    val dslBlock = BlockBuilder.buildOpen {
      val value = Var("value", TypeRef.of(propertyType))
      val context = Var("context", Types.SerializationContext)

      if (property.settings().isNullable()) {
        ifThen(value.isNull) {
          return_(Expr.staticCall(Types.DataTree, "null_"))
        }
      }

      if (useObjectMapper) {
        return_(
          Expr.staticCall(
            Types.DataTreeTransforms,
            "loadFrom",
            context.call("getMapper").call("map", value)
          )
        )
      } else {
        val result = declare(
          Types.DataTree,
          "result",
          generateSerialization(property.propertyType(), value, 0)
        )
        return_(result)
      }
    }

    builder.addCode(CodeBlockRenderer.render(dslBlock))
    builder.build()

  private def isKnownSerializableType(wrappedType: TypeMirrorWrapper): Boolean =
    if (wrappedType.getTypeElement.isEmpty && wrappedType.isPrimitive) {
      return true
    }

    if (
      typesUtil.getDataTreeType(TypeName.get(wrappedType.unwrap())).isDefined
    ) {
      return true
    }

    if (wrappedType.getQualifiedName == classOf[java.lang.Character].getName) {
      return true
    }
    wrappedType.isEnum

  private def generateSerialization(
      tpe: TypeMirror,
      input: Expr[?],
      depth: Int
  )(using
      BlockBuilder
  ): Expr[?] =
    val wrappedType = TypeMirrorWrapper.wrap(tpe)

    // Custom Serializer
    val customSerializerOptional = customSerializers.getCustomInfo(tpe)
    if (customSerializerOptional.isPresent) {
      val info = customSerializerOptional.get()
      val publicTypeName =
        configurationClassNameGenerator.publicPropertyClassName(tpe)
      val context = Var("context", Types.SerializationContext)
      if (info.isStatic) {
        Expr.staticCall(
          TypeRef.of(ClassName.get(info.serializerClass)),
          "serialize",
          input.cast(TypeRef.of(publicTypeName)),
          context
        )
      } else {
        val fieldName = Strings.uncapitalize(
          info.serializerClass.getSimpleName.toString
        ) + ConfigurationClassNameGenerator.PROVIDER_SUFFIX
        Expr.This
          .field(fieldName)
          .call("get")
          .call("apply", input.cast(TypeRef.of(publicTypeName)), context)
      }
    }
    // Newtype
    else if (typesUtil.isNewtype(tpe)) {
      val typeElement =
        tpe.asInstanceOf[DeclaredType].asElement().asInstanceOf[TypeElement]
      val underlyingType = typesUtil.getNewtypeUnderlyingType(tpe)
      val accessor =
        NewtypeUtil.getAccessorName(typeElement).replaceAll("\\(\\)", "")
      val unwrappedVarName = input match {
        case Var(name, _) => s"${name}_${depth}_unwrapped"
        case _            => s"value_${depth}_unwrapped"
      }
      val unwrappedVar = declare(
        TypeRef.of(TypeName.get(underlyingType)),
        unwrappedVarName,
        input.call(accessor)
      )
      generateSerialization(underlyingType, unwrappedVar, depth + 1)
    }
    // Config type
    else if (typesUtil.isConfigType(tpe)) {
      val saverFieldName =
        configurationClassNameGenerator.getSerializerProviderFieldName(tpe)
      val publicTypeName =
        configurationClassNameGenerator.publicPropertyClassName(tpe)
      val context = Var("context", Types.SerializationContext)
      Expr.This
        .field(saverFieldName)
        .call("get")
        .call("apply", input.cast(TypeRef.of(publicTypeName)), context)
    }
    // Generic collections (List, Map, Set)
    else if (wrappedType.hasTypeArguments) {
      val canonicalName = wrappedType.erasure().getQualifiedName()
      val context = Var("context", Types.SerializationContext)
      if (
        canonicalName == classOf[
          java.util.List[?]
        ].getName || canonicalName == classOf[java.util.Set[?]].getName
      ) {
        val elementType = wrappedType.getTypeArguments.get(0)
        val serializeHelper =
          if (canonicalName == classOf[java.util.List[?]].getName)
            "serializeList"
          else "serializeSet"
        val elVar = Var(s"el$depth", TypeRef.of(TypeName.get(elementType)))
        val ctxVar = Var(s"ctx$depth", Types.SerializationContext)

        val lambdaBlock = BlockBuilder.build {
          val elementTarget = declare(
            Types.DataTree,
            s"res$depth",
            generateSerialization(elementType, elVar, depth + 1)
          )
          return_(elementTarget)
        }

        Expr.staticCall(
          Types.CollectionsUtils,
          serializeHelper,
          input,
          context,
          Expr.Lambda(scala.List(elVar, ctxVar), lambdaBlock)
        )
      } else if (canonicalName == classOf[java.util.Map[?, ?]].getName) {
        val valueType = wrappedType.getTypeArguments.get(1)
        val valVar = Var(s"val$depth", TypeRef.of(TypeName.get(valueType)))
        val ctxVar = Var(s"ctx$depth", Types.SerializationContext)

        val lambdaBlock = BlockBuilder.build {
          val valTarget = declare(
            Types.DataTree,
            s"mapVal$depth",
            generateSerialization(valueType, valVar, depth + 1)
          )
          return_(valTarget)
        }

        Expr.staticCall(
          Types.CollectionsUtils,
          "serializeMap",
          input,
          context,
          Expr.Lambda(scala.List(valVar, ctxVar), lambdaBlock)
        )
      } else {
        throw new IllegalStateException(
          "Unexpected generic type: " + canonicalName
        )
      }
    }
    // Enum type
    else if (wrappedType.isEnum) {
      Expr.staticCall(
        Types.DataTreeTransforms,
        "loadFrom",
        Expr.Ternary(input === Expr.Null, Expr.Null, input.call("name"))
      )
    }
    // Basic fallback
    else {
      Expr.staticCall(Types.DataTreeTransforms, "loadFrom", input)
    }
