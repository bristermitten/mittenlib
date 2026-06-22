package me.bristermitten.mittenlib.annotations.compile

import com.google.gson.reflect.TypeToken
import com.google.inject.Inject
import com.google.inject.Provider
import com.palantir.javapoet.*
import io.toolisticon.aptk.tools.TypeMirrorWrapper
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.{
  Collections => JCollections,
  LinkedHashMap => JLinkedHashMap,
  LinkedHashSet => JLinkedHashSet,
  List => JList,
  Map => JMap,
  Set => JSet
}
import javax.annotation.processing.Generated
import javax.lang.model.element.{Modifier, TypeElement}
import javax.lang.model.`type`.TypeMirror
import me.bristermitten.mittenlib.annotations.ast.{
  AbstractConfigStructure,
  Property
}
import me.bristermitten.mittenlib.annotations.config.ConfigProcessor
import me.bristermitten.mittenlib.annotations.parser.CustomSerializers
import me.bristermitten.mittenlib.annotations.util.TypesUtil
import me.bristermitten.mittenlib.config.{
  SerializationContext,
  SerializationFunction
}
import me.bristermitten.mittenlib.config.tree.DataTree
import me.bristermitten.mittenlib.util.Strings
import scala.jdk.CollectionConverters.*

import _root_.me.bristermitten.mittenlib.codegen.dsl.*
import _root_.me.bristermitten.mittenlib.codegen.dsl.BlockBuilder.*
import _root_.me.bristermitten.mittenlib.codegen.dsl.given

class ConfigSaverGenerator @Inject() (
    private val classNameGenerator: ConfigurationClassNameGenerator,
    private val serializationCodeGenerator: SerializationCodeGenerator,
    private val methodNames: MethodNames,
    private val typesUtil: TypesUtil,
    private val fieldNameGenerator: FieldNameGenerator,
    private val configNameCache: ConfigNameCache,
    private val customSerializers: CustomSerializers
):

  /** Entry point for generating a {@@@@@linkJavaFile} for a configuration
    * saver.
    *
    * @param ast
    *   the configuration structure to generate a saver for
    * @return
    *   a {@@@@@linkJavaFile} containing the generated saver class
    */
  def emit(ast: AbstractConfigStructure): JavaFile =
    val saverClassName = classNameGenerator.getSerializerClassName(ast)
    val builder = createSaverBuilder(ast)
    JavaFile
      .builder(saverClassName.packageName(), builder.build())
      .skipJavaLangImports(true)
      .build()

  /** Creates the {@@@@@linkTypeSpec.Builder} for the saver class, including its
    * annotations, constructor, fields, and serialization methods.
    *
    * @param ast
    *   the configuration structure
    * @return
    *   a builder for the saver class
    */
  private def createSaverBuilder(
      ast: AbstractConfigStructure
  ): TypeSpec.Builder =
    val publicClassName = classNameGenerator.getPublicClassName(ast)
    val saverClassName = classNameGenerator.getSerializerClassName(ast)

    val builder = TypeSpec
      .classBuilder(saverClassName)
      .addJavadoc(
        """Serializer implementation for {@link $T}.
          |""".stripMargin,
        publicClassName
      )
      .addModifiers(Modifier.PUBLIC)
      .addSuperinterface(
        ParameterizedTypeName.get(
          ClassName.get(classOf[SerializationFunction[?]]),
          publicClassName
        )
      )

    builder.addAnnotation(GeneratorUtil.generatedAnnotation(true))

    val constructorBuilder = MethodSpec
      .constructorBuilder()
      .addJavadoc("Constructs a new serializer instance.\n")
      .addAnnotation(classOf[Inject])
      .addModifiers(Modifier.PUBLIC)

    // Add child savers as dependencies recursively
    for (property <- ast.properties().asScala) {
      collectSaverDependencies(
        property.propertyType(),
        builder,
        constructorBuilder
      )
    }

    // Add custom serializers as dependencies recursively
    val injectedTypes = new JLinkedHashSet[TypeName]()
    val injectedFieldNames = new JLinkedHashMap[TypeName, String]()
    for (property <- ast.properties().asScala) {
      collectCustomSerializers(
        property.propertyType(),
        injectedTypes,
        injectedFieldNames
      )
    }

    for (typeName <- injectedTypes.asScala) {
      val fieldName = injectedFieldNames.get(typeName)
      val providerType =
        ParameterizedTypeName.get(ClassName.get(classOf[Provider[?]]), typeName)
      builder.addField(
        FieldSpec
          .builder(providerType, fieldName, Modifier.PRIVATE, Modifier.FINAL)
          .build()
      )
      constructorBuilder.addParameter(providerType, fieldName)
      constructorBuilder.addStatement("this.$L = $L", fieldName, fieldName)
    }

    builder.addMethod(constructorBuilder.build())

    // Implement apply method
    val applyMethod = MethodSpec
      .methodBuilder("apply")
      .addJavadoc(
        """Serializes the configuration instance into a {@link $T}.
          |
          |@param config the configuration instance to serialize
          |@param context the serialization context
          |@return the serialized DataTree representation
          |""".stripMargin,
        classOf[DataTree]
      )
      .addAnnotation(classOf[Override])
      .addModifiers(Modifier.PUBLIC)
      .returns(classOf[DataTree])
      .addParameter(publicClassName, "config")
      .addParameter(classOf[SerializationContext], "context")

    val applyBlock = BlockBuilder.buildOpen {
      val config = Var("config", TypeRef.of(publicClassName))
      val context = StagedExpr
        .param[SerializationContext](Types.SerializationContext, "context")

      val mapType = Types.Map(Types.DataTree, Types.DataTree)
      val map = declare(
        mapType,
        "map",
        Expr.new_(TypeRef.of(classOf[JLinkedHashMap[?, ?]]))
      )

      for (property <- ast.properties().asScala) {
        val key = fieldNameGenerator.getConfigFieldName(property)
        val serializeMethodName = methodNames.getSerializeMethodName(property)

        // Get the property value based on source type
        val propAccessExpr = GeneratorUtil.getPropertyAccess(
          ast,
          property,
          config,
          methodNames,
          true
        )

        statement(
          map.call(
            "put",
            Expr.staticCall(Types.DataTree, "string", Expr.str(key)),
            Expr.This.call(serializeMethodName, propAccessExpr, context)
          )
        )
      }
      return_(Expr.staticCall(Types.DataTree, "map", map))
    }

    applyMethod.addCode(CodeBlockRenderer.render(applyBlock))
    builder.addMethod(applyMethod.build())

    addGenerateDefaultMethod(ast, builder)

    // Add private serialize methods for each property
    serializationCodeGenerator.addSerializeMethodsToSaver(builder, ast)

    // Add nested classes
    for (enclosed <- ast.enclosed().asScala) {
      builder.addType(
        createSaverBuilder(enclosed).addModifiers(Modifier.STATIC).build()
      )
    }

    builder

  /** Adds the {@@@@@codegenerateDefault} method to the saver, which creates a
    * default {@@@@@linkDataTree} for the configuration structure, using a DAO
    * for default values when possible.
    */
  private def addGenerateDefaultMethod(
      ast: AbstractConfigStructure,
      builder: TypeSpec.Builder
  ): Unit =
    val method = MethodSpec
      .methodBuilder("generateDefault")
      .addJavadoc(
        """Generates a default {@link $T} representation of the configuration,
          |populating default values using method defaults.
          |
          |@param context the serialization context
          |@return the default DataTree representation
          |""".stripMargin,
        classOf[DataTree]
      )
      .addAnnotation(classOf[Override])
      .addModifiers(Modifier.PUBLIC)
      .returns(classOf[DataTree])
      .addParameter(classOf[SerializationContext], "context")

    val daoName = GeneratorUtil.getDaoName(ast, classNameGenerator)

    val dslBlock = BlockBuilder.buildOpen {
      val context = StagedExpr
        .param[SerializationContext](Types.SerializationContext, "context")
      val daoVar =
        if (
          daoName != null && ast
            .properties()
            .asScala
            .exists(_.settings().hasDefaultValue())
        ) {
          Some(
            declare(TypeRef.of(daoName), "dao", Expr.new_(TypeRef.of(daoName)))
          )
        } else {
          None
        }
      val mapType = Types.Map(Types.DataTree, Types.DataTree)
      val map = declare(
        mapType,
        "map",
        Expr.new_(TypeRef.of(classOf[JLinkedHashMap[?, ?]]))
      )

      for (property <- ast.properties().asScala) {
        val key = fieldNameGenerator.getConfigFieldName(property)
        val propertyType = property.propertyType()

        if (property.settings().hasDefaultValue()) {
          val propAccessExpr = GeneratorUtil.getPropertyAccess(
            ast,
            property,
            daoVar.get,
            methodNames,
            false
          )

          if (hasConfigType(propertyType)) {
            statement(
              map.call(
                "put",
                Expr.staticCall(Types.DataTree, "string", Expr.str(key)),
                context.getMapper
                  .map(
                    propAccessExpr,
                    Expr.newAnonymous(Types.TypeToken(Types.DataTree))
                  )
                  .getOrThrow
              )
            )
          } else {
            val serializeMethodName =
              methodNames.getSerializeMethodName(property)
            statement(
              map.call(
                "put",
                Expr.staticCall(Types.DataTree, "string", Expr.str(key)),
                Expr.This.call(serializeMethodName, propAccessExpr, context)
              )
            )
          }
        } else if (typesUtil.isConfigType(propertyType)) {
          if (property.settings().isNullable()) {
            statement(
              map.call(
                "put",
                Expr.staticCall(Types.DataTree, "string", Expr.str(key)),
                Expr.staticCall(Types.DataTree, "null_")
              )
            )
          } else {
            val saverFieldName =
              classNameGenerator.getSerializerProviderFieldName(propertyType)
            statement(
              map.call(
                "put",
                Expr.staticCall(Types.DataTree, "string", Expr.str(key)),
                Expr.This
                  .field(saverFieldName)
                  .call("get")
                  .call("generateDefault", context)
              )
            )
          }
        } else {
          val wrapped = TypeMirrorWrapper.wrap(propertyType)
          if (wrapped.hasTypeArguments()) {
            val canonicalName = wrapped.erasure().getQualifiedName()
            if (canonicalName == classOf[JList[?]].getName) {
              statement(
                map.call(
                  "put",
                  Expr.staticCall(Types.DataTree, "string", Expr.str(key)),
                  Expr.staticCall(Types.DataTree, "array")
                )
              )
            } else if (canonicalName == classOf[JMap[?, ?]].getName) {
              statement(
                map.call(
                  "put",
                  Expr.staticCall(Types.DataTree, "string", Expr.str(key)),
                  Expr.staticCall(
                    Types.DataTree,
                    "map",
                    Expr
                      .staticCall(TypeRef.of(classOf[JCollections]), "emptyMap")
                  )
                )
              )
            } else {
              statement(
                map.call(
                  "put",
                  Expr.staticCall(Types.DataTree, "string", Expr.str(key)),
                  Expr.staticCall(Types.DataTree, "null_")
                )
              )
            }
          } else {
            statement(
              map.call(
                "put",
                Expr.staticCall(Types.DataTree, "string", Expr.str(key)),
                Expr.staticCall(Types.DataTree, "null_")
              )
            )
          }
        }
      }
      return_(Expr.staticCall(Types.DataTree, "map", map))
    }

    method.addCode(CodeBlockRenderer.render(dslBlock))
    builder.addMethod(method.build())

  /** Adds a dependency on another configuration saver to the class fields and
    * constructor.
    */
  private def addSaverDependency(
      builder: TypeSpec.Builder,
      constructorBuilder: MethodSpec.Builder,
      tpe: TypeMirror
  ): Unit =
    val astOpt = configNameCache.lookupAST(tpe)
    if (astOpt.isEmpty) return
    val ast = astOpt.get()

    val publicChildClassName = classNameGenerator.getPublicClassName(ast)
    val fieldName = classNameGenerator.getSerializerProviderFieldName(tpe)

    if (builder.build().fieldSpecs().asScala.exists(_.name == fieldName)) {
      return
    }

    val serializationFunctionType = ParameterizedTypeName.get(
      ClassName.get(classOf[SerializationFunction[?]]),
      publicChildClassName
    )
    val providerType = ParameterizedTypeName.get(
      ClassName.get(classOf[Provider[?]]),
      serializationFunctionType
    )

    builder.addField(providerType, fieldName, Modifier.PRIVATE, Modifier.FINAL)

    constructorBuilder.addParameter(providerType, fieldName)
    constructorBuilder.addStatement("this.$L = $L", fieldName, fieldName)

  /** Recursively traverses generic type arguments of a property's type to
    * discover configuration savers that need to be injected into the generated
    * saver as dependencies.
    */
  private def collectSaverDependencies(
      tpe: TypeMirror,
      builder: TypeSpec.Builder,
      constructorBuilder: MethodSpec.Builder
  ): Unit =
    if (typesUtil.isConfigType(tpe)) {
      addSaverDependency(builder, constructorBuilder, tpe)
      return
    }

    val wrapped = TypeMirrorWrapper.wrap(tpe)
    if (wrapped.hasTypeArguments) {
      for (arg <- wrapped.getTypeArguments.asScala) {
        collectSaverDependencies(arg, builder, constructorBuilder)
      }
    }

  /** Recursively traverses generic type arguments of a property's type to
    * discover non-static custom serializers that need to be injected into the
    * generated saver.
    */
  private def collectCustomSerializers(
      tpe: TypeMirror,
      injectedTypes: JSet[TypeName],
      injectedFieldNames: JMap[TypeName, String]
  ): Unit =
    customSerializers
      .getCustomInfo(tpe)
      .ifPresent(info => {
        if (!info.isStatic) {
          val serializerClass = info.serializerClass
          val serializerClassName = ClassName.get(serializerClass)
          val fieldName = Strings.uncapitalize(
            serializerClass.getSimpleName.toString
          ) + ConfigurationClassNameGenerator.PROVIDER_SUFFIX
          if (injectedTypes.add(serializerClassName)) {
            injectedFieldNames.put(serializerClassName, fieldName)
          }
        }
      })

    val wrapped = TypeMirrorWrapper.wrap(tpe)
    if (wrapped.hasTypeArguments) {
      for (arg <- wrapped.getTypeArguments.asScala) {
        collectCustomSerializers(arg, injectedTypes, injectedFieldNames)
      }
    }

  private def hasConfigType(tpe: TypeMirror): Boolean =
    if (typesUtil.isConfigType(tpe)) {
      return true
    }
    val wrapped = TypeMirrorWrapper.wrap(tpe)
    if (wrapped.hasTypeArguments) {
      for (arg <- wrapped.getTypeArguments.asScala) {
        if (hasConfigType(arg)) {
          return true
        }
      }
    }
    false
