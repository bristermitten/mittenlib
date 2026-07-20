package me.bristermitten.mittenlib.annotations.compile

import _root_.me.bristermitten.mittenlib.codegen.dsl.BlockBuilder.*
import _root_.me.bristermitten.mittenlib.codegen.dsl.{*, given}
import com.google.inject.{Inject, Provider}
import com.palantir.javapoet.*
import io.toolisticon.aptk.tools.TypeMirrorWrapper
import me.bristermitten.mittenlib.annotations.ast.{
  AbstractConfigStructure,
  ConfigTypeSource,
  CustomDeserializerInfo,
  Property
}
import me.bristermitten.mittenlib.annotations.parser.CustomDeserializers
import me.bristermitten.mittenlib.annotations.util.{
  ConfigStructureAnalysis,
  TypesUtil
}
import me.bristermitten.mittenlib.config.exception.ConfigLoadingErrors
import me.bristermitten.mittenlib.config.{
  DeserializationContext,
  DeserializationFunction
}
import me.bristermitten.mittenlib.util.{Result, Strings}
import org.jspecify.annotations.Nullable

import java.util.{
  ArrayList as JArrayList,
  LinkedHashMap as JLinkedHashMap,
  LinkedHashSet as JLinkedHashSet,
  List as JList,
  Map as JMap,
  Set as JSet
}
import javax.lang.model.`type`.TypeMirror
import javax.lang.model.element.Modifier
import scala.jdk.CollectionConverters.*

class ConfigLoaderGenerator @Inject() (
    private val classNameGenerator: ConfigurationClassNameGenerator,
    private val deserializationCodeGenerator: DeserializationCodeGenerator,
    private val customDeserializers: CustomDeserializers,
    private val typesUtil: TypesUtil,
    private val configStructureAnalysis: ConfigStructureAnalysis
):

  /** Entry point for generating a [[JavaFile]] for a configuration loader.
    *
    * @param ast
    *   the configuration structure to generate a loader for
    * @return
    *   a [[JavaFile]] containing the generated loader class
    */
  def emit(ast: AbstractConfigStructure): JavaFile =
    val loaderClassName = classNameGenerator.getDeserializerClassName(ast)
    val builder = createLoaderBuilder(ast)
    addChildLoaderClasses(ast, builder)

    JavaFile
      .builder(loaderClassName.packageName(), builder.build())
      .skipJavaLangImports(true)
      .build()

  /** Creates the [[TypeSpec.Builder]] for the loader class, including its
    * annotations, fields, constructor, and deserialization methods.
    *
    * @param ast
    *   the configuration structure
    * @return
    *   a builder for the loader class
    */
  private def createLoaderBuilder(
      ast: AbstractConfigStructure
  ): TypeSpec.Builder =
    val loaderClassName = classNameGenerator.getDeserializerClassName(ast)
    val publicClassName = classNameGenerator.getPublicClassName(ast)

    val builder = TypeSpec
      .classBuilder(loaderClassName.simpleName())
      .addJavadoc(
        """Deserializer implementation for [[$T]].
          |""".stripMargin,
        publicClassName
      )
      .addModifiers(Modifier.PUBLIC)
      .addSuperinterface(
        ParameterizedTypeName.get(
          ClassName.get(classOf[DeserializationFunction[?]]),
          publicClassName
        )
      )

    if (ast.enclosedIn() != null) {
      builder.addModifiers(Modifier.STATIC)
    }

    builder.addAnnotation(GeneratorUtil.generatedAnnotation())

    // Collect fields & constructor dependencies
    addFieldsAndConstructor(ast, builder)

    // Add property deserialization methods
    val daoName = GeneratorUtil.getDaoName(ast, classNameGenerator)
    val dtoType = ast.source().element()
    val deserializeMethods = ast
      .properties()
      .asScala
      .map(property =>
        deserializationCodeGenerator
          .createDeserializeMethodFor(dtoType, ast, property, daoName)
      )
      .toList

    deserializeMethods.foreach(builder.addMethod)

    // Implement apply method
    addApplyMethod(ast, builder, deserializeMethods.asJava, daoName)

    builder

  /** Recursively adds nested loader classes for enclosed configuration
    * structures.
    */
  private def addChildLoaderClasses(
      ast: AbstractConfigStructure,
      loaderBuilder: TypeSpec.Builder
  ): Unit =
    for (child <- ast.enclosed().asScala) {
      val childLoaderBuilder = createLoaderBuilder(child)
      addChildLoaderClasses(child, childLoaderBuilder)
      loaderBuilder.addType(childLoaderBuilder.build())
    }

  /** Adds fields and a {@@@@@code@Inject} constructor to the loader class for
    * its dependencies.
    */
  private def addFieldsAndConstructor(
      ast: AbstractConfigStructure,
      builder: TypeSpec.Builder
  ): Unit =
    val constructor = MethodSpec
      .constructorBuilder()
      .addJavadoc("Constructs a new deserializer/loader instance.\n")
      .addAnnotation(classOf[Inject])
      .addModifiers(Modifier.PUBLIC)

    // Injected Validator if validation is needed
    if (configStructureAnalysis.needsValidation(ast)) {
      val validatorClassName = classNameGenerator.getValidatorClassName(ast)
      val validatorFieldName = "validator"
      builder.addField(
        FieldSpec
          .builder(
            validatorClassName,
            validatorFieldName,
            Modifier.PRIVATE,
            Modifier.FINAL
          )
          .build()
      )
      constructor.addParameter(validatorClassName, validatorFieldName)
      constructor.addStatement(
        "this.$L = $L",
        validatorFieldName,
        validatorFieldName
      )
    }

    // Referenced Sub-loaders & Non-static Custom Deserializers
    val injectedTypes = new JLinkedHashSet[TypeName]()
    val injectedFieldNames = new JLinkedHashMap[TypeName, String]()

    for (parent <- ast.source().parents().asScala) {
      collectConfigTypes(parent, injectedTypes, injectedFieldNames)
    }

    ast match {
      case union: AbstractConfigStructure.Union =>
        for (alternative <- union.alternatives.asScala) {
          collectConfigTypes(
            alternative.source().element().asType(),
            injectedTypes,
            injectedFieldNames
          )
        }
      case _ =>
    }

    for (property <- ast.properties().asScala) {
      collectCustomDeserializers(
        property.propertyType(),
        injectedTypes,
        injectedFieldNames
      )
      collectConfigTypes(
        property.propertyType(),
        injectedTypes,
        injectedFieldNames
      )
    }

    for (typeName <- injectedTypes.asScala) {
      val fieldName = injectedFieldNames.get(typeName)
      builder.addField(
        FieldSpec
          .builder(typeName, fieldName, Modifier.PRIVATE, Modifier.FINAL)
          .build()
      )
      constructor.addParameter(typeName, fieldName)
      constructor.addStatement("this.$L = $L", fieldName, fieldName)
    }

    builder.addMethod(constructor.build())

  /** Collects configuration types that need to be injected as sub-loaders.
    */
  private def collectConfigTypes(
      tpe: TypeMirror,
      injectedTypes: JSet[TypeName],
      injectedFieldNames: JMap[TypeName, String]
  ): Unit =
    if (typesUtil.isConfigType(tpe)) {
      val subLoaderName = classNameGenerator.getDeserializerClassName(tpe)
      val providerType = ParameterizedTypeName.get(
        ClassName.get(classOf[Provider[?]]),
        subLoaderName
      )
      val fieldName = classNameGenerator.getDeserializerProviderFieldName(tpe)
      if (injectedTypes.add(providerType)) {
        injectedFieldNames.put(providerType, fieldName)
      }
      return
    }

    val wrapped = TypeMirrorWrapper.wrap(tpe)
    if (wrapped.hasTypeArguments) {
      for (arg <- wrapped.getTypeArguments.asScala) {
        collectConfigTypes(arg, injectedTypes, injectedFieldNames)
      }
    }

  /** Recursively traverses generic type arguments of a property's type to
    * discover non-static custom deserializers that need to be injected into the
    * generated loader.
    */
  private def collectCustomDeserializers(
      tpe: TypeMirror,
      injectedTypes: JSet[TypeName],
      injectedFieldNames: JMap[TypeName, String]
  ): Unit =
    customDeserializers
      .getCustomInfo(tpe)
      .ifPresent(info => {
        if (!info.isStatic) {
          val deserializerClass = info.deserializerClass
          val fieldName =
            Strings.uncapitalize(deserializerClass.getSimpleName.toString)
          val deserializerClassName = ClassName.get(deserializerClass)
          if (injectedTypes.add(deserializerClassName)) {
            injectedFieldNames.put(deserializerClassName, fieldName)
          }
        }
      })

    val wrapped = TypeMirrorWrapper.wrap(tpe)
    if (wrapped.hasTypeArguments) {
      for (arg <- wrapped.getTypeArguments.asScala) {
        collectCustomDeserializers(arg, injectedTypes, injectedFieldNames)
      }
    }

  /** Adds the {@@@@@codeapply} method to the loader, which orchestrates the
    * deserialization of all properties.
    */
  private def addApplyMethod(
      ast: AbstractConfigStructure,
      builder: TypeSpec.Builder,
      deserializeMethods: JList[MethodSpec],
      @Nullable daoName: ClassName
  ): Unit =
    val publicClassName = classNameGenerator.getPublicClassName(ast)
    val implClassName = classNameGenerator.translateConfigClassName(ast)

    val applyMethod = MethodSpec
      .methodBuilder("apply")
      .addJavadoc(
        """Deserializes the configuration from the given [[$T]].
          |
          |@param context the context containing the raw data and mapper
          |@return a [[$T]] containing either the successfully deserialized configuration or a failure
          |""".stripMargin,
        classOf[DeserializationContext],
        classOf[Result[?]]
      )
      .addAnnotation(classOf[Override])
      .addAnnotation(
        AnnotationSpec
          .builder(classOf[SuppressWarnings])
          .addMember("value", "$S", "unchecked")
          .build()
      )
      .addModifiers(Modifier.PUBLIC)
      .returns(
        ParameterizedTypeName
          .get(ClassName.get(classOf[Result[?]]), publicClassName)
      )
      .addParameter(classOf[DeserializationContext], "context", Modifier.FINAL)

    val dslBlock = BlockBuilder.buildOpen {
      val context = StagedExpr
        .param[DeserializationContext](Types.DeserializationContext, "context")

      tryCatch(Types.Exception) {
        ast match {
          case union: AbstractConfigStructure.Union =>
            for (
              (alternative, idx) <- union.alternatives.asScala.zipWithIndex
            ) {
              val loaderFieldName =
                classNameGenerator.getDeserializerProviderFieldName(
                  alternative.source().element().asType()
                )
              val varName = s"var$idx"
              val alternativePublicType =
                classNameGenerator.getPublicClassName(alternative)
              val resultType = Types.Result(
                TypeRef.of(WildcardTypeName.subtypeOf(alternativePublicType))
              )
              val varRes = declare(
                resultType,
                s"${varName}Res",
                Expr.This
                  .field(loaderFieldName)
                  .call("get")
                  .call("apply", context)
              )

              ifThen(varRes.call("isSuccess")) {
                return_(varRes.cast(Types.Result(TypeRef.of(publicClassName))))
              }
            }
            return_(
              Expr.staticCall(
                Types.Result,
                "fail",
                Expr.staticCall(
                  TypeRef.of(classOf[ConfigLoadingErrors]),
                  "noUnionMatch"
                )
              )
            )

          case _ =>
            val hasAnyDefault =
              ast
                .properties()
                .asScala
                .exists(configStructureAnalysis.hasDefaultOrIsInitializable)
            val daoVarOpt = if (hasAnyDefault && daoName != null) {
              Some(
                declare(
                  TypeRef.of(daoName),
                  "dao",
                  Expr.new_(TypeRef.of(daoName))
                )
              )
            } else {
              None
            }

            val constructorVars = new JArrayList[Var[?]]()
            val superClass = ast.source() match {
              case c: ConfigTypeSource.ClassConfigTypeSource =>
                if (c.parentField.isPresent) Some(c.parentField.get()) else None
              case _ => None
            }

            var i = 0
            if (superClass.isDefined) {
              val parentType = superClass.get
              val varName = s"var$i"
              i += 1

              val superLoaderFieldName =
                classNameGenerator.getDeserializerProviderFieldName(parentType)
              val superPublicType =
                classNameGenerator.publicPropertyClassName(parentType)

              val varRes = declare(
                Types.Result(TypeRef.of(superPublicType)),
                s"${varName}Res",
                Expr.This
                  .field(superLoaderFieldName)
                  .call("get")
                  .call("apply", context)
              )

              ifThen(varRes.call("isFailure")) {
                return_(varRes.cast(Types.Result(TypeRef.of(publicClassName))))
              }

              val parentVar = declare(
                TypeRef.of(superPublicType),
                varName,
                varRes.call("getOrThrow")
              )
              constructorVars.add(parentVar)
            }

            for ((property, idx) <- ast.properties().asScala.zipWithIndex) {
              val varName = s"var$i"
              i += 1

              val deserializeMethod = deserializeMethods.get(idx)
              val returnType = deserializeMethod.returnType()
              val innerType = returnType match {
                case pt: ParameterizedTypeName => pt.typeArguments().get(0)
                case _ => TypeName.get(property.propertyType())
              }

              val deserializeMethodArguments: List[Expr[?]] =
                if (
                  daoName != null && configStructureAnalysis
                    .hasDefaultOrIsInitializable(property)
                ) {
                  List(context, daoVarOpt.get)
                } else {
                  List(context)
                }

              val varRes = declare(
                TypeRef.of(returnType),
                s"${varName}Res",
                Expr.This
                  .call(deserializeMethod.name, deserializeMethodArguments*)
              )

              ifThen(varRes.call("isFailure")) {
                return_(varRes.cast(Types.Result(TypeRef.of(publicClassName))))
              }

              val propVar = declare(
                TypeRef.of(innerType),
                varName,
                varRes.call("getOrThrow")
              )
              constructorVars.add(propVar)
            }

            val constructorCall = Expr.new_(
              TypeRef.of(implClassName),
              constructorVars.asScala.toList*
            )

            if (configStructureAnalysis.needsValidation(ast)) {
              return_(
                Expr
                  .staticCall(Types.Result, "ok", constructorCall)
                  .call(
                    "flatMap",
                    Expr.methodRef(Expr.This.field("validator"), "validate")
                  )
                  .cast(Types.Result(TypeRef.of(publicClassName)))
              )
            } else {
              return_(
                Expr.staticCall(
                  Types.Result,
                  "ok",
                  constructorCall.cast(TypeRef.of(publicClassName))
                )
              )
            }
        }
      } { e =>
        return_(Expr.staticCall(Types.Result, "fail", e))
      }
    }

    applyMethod.addCode(CodeBlockRenderer.render(dslBlock))
    builder.addMethod(applyMethod.build())
