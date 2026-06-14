package me.bristermitten.mittenlib.annotations.compile

import com.google.inject.AbstractModule
import com.google.inject.Binder
import com.google.inject.Inject
import com.google.inject.Provides
import com.google.inject.Singleton
import com.google.inject.TypeLiteral
import com.google.inject.multibindings.ProvidesIntoSet
import com.palantir.javapoet.*
import io.toolisticon.aptk.tools.MessagerUtils
import java.util.{List => JList, Map => JMap, Set => JSet}
import javax.annotation.processing.Generated
import javax.lang.model.element.Modifier
import me.bristermitten.mittenlib.annotations.ast.{AbstractConfigStructure, Property}
import me.bristermitten.mittenlib.annotations.util.ConfigStructureAnalysis
import me.bristermitten.mittenlib.config.BindProperty
import me.bristermitten.mittenlib.config.Configuration
import me.bristermitten.mittenlib.config.DeserializationFunction
import me.bristermitten.mittenlib.config.MittenLibConfigLoader
import me.bristermitten.mittenlib.config.SerializationFunction
import me.bristermitten.mittenlib.config.provider.ConfigProvider
import me.bristermitten.mittenlib.config.provider.SaveableConfigProvider
import me.bristermitten.mittenlib.config.provider.construct.ConfigProviderFactory
import me.bristermitten.mittenlib.config.provider.construct.ConfigProviderImprover
import org.jspecify.annotations.Nullable
import scala.jdk.CollectionConverters.*

class ConfigLoaderModuleGenerator @Inject() (
  private val classNameGenerator: ConfigurationClassNameGenerator,
  private val methodNames: MethodNames,
  private val configStructureAnalysis: ConfigStructureAnalysis
):

  def emit(asts: JList[AbstractConfigStructure], rootPackage: String): JavaFile =
    if (asts.isEmpty) {
      throw new IllegalArgumentException("asts list cannot be empty")
    }

    val moduleClassName = classNameGenerator.getLoaderModuleClassName(rootPackage)

    val builder = TypeSpec.classBuilder(moduleClassName)
      .addJavadoc("Generated Guice module for loading configurations.\n"
        + "This module should be installed in your application's injector.")
      .addModifiers(Modifier.PUBLIC)
      .superclass(classOf[MittenLibConfigLoader])
      .addAnnotation(AnnotationSpec.builder(classOf[Generated])
        .addMember("value", "$S", "me.bristermitten.mittenlib.annotations.config.ConfigProcessor")
        .build())

    val configureMethod = MethodSpec.methodBuilder("configure")
      .addJavadoc(
        """Configures Guice bindings for serializer and deserializer functions.
          |
          |@param binder the Guice binder
          |""".stripMargin)
      .addAnnotation(classOf[Override])
      .addModifiers(Modifier.PROTECTED)
      .addParameter(classOf[Binder], "binder")

    // Bindings for functions
    for (ast <- asts.asScala) {
      addFunctionBindings(configureMethod, ast)
    }

    builder.addMethod(configureMethod.build())

    val internalModuleBuilder = TypeSpec.classBuilder("GeneratedModule")
      .addJavadoc("Internal module for providing configuration instances.")
      .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
      .superclass(classOf[AbstractModule])
      .addMethod(MethodSpec.methodBuilder("configure")
        .addAnnotation(classOf[Override])
        .addModifiers(Modifier.PROTECTED)
        .addStatement("$T.this.configure(binder())", moduleClassName)
        .build())

    // Provides methods for providers and configs
    for (ast <- asts.asScala) {
      addProvidesMethods(internalModuleBuilder, ast, null, false)
    }

    builder.addType(internalModuleBuilder.build())

    val asModuleMethod = MethodSpec.methodBuilder("asModule")
      .addJavadoc(
        """Returns the Guice module containing all provides methods and configuration bindings.
          |
          |@return the Guice module instance
          |""".stripMargin)
      .addAnnotation(classOf[Override])
      .addModifiers(Modifier.PUBLIC)
      .returns(classOf[com.google.inject.Module])
      .addStatement("return new GeneratedModule()")
      .build()

    builder.addMethod(asModuleMethod)

    JavaFile.builder(moduleClassName.packageName(), builder.build())
      .skipJavaLangImports(true)
      .build()

  private def addFunctionBindings(configureMethod: MethodSpec.Builder, ast: AbstractConfigStructure): Unit =
    val publicClassName = classNameGenerator.getPublicClassName(ast)
    val loaderClassName = classNameGenerator.getDeserializerClassName(ast)
    val saverClassName = classNameGenerator.getSerializerClassName(ast)

    configureMethod.addCode("\n")
    configureMethod.addComment("Function bindings for $T", publicClassName)

    // Bind DeserializationFunction<Public> -> Loader
    configureMethod.addStatement(
      "binder.bind(new $1T<$2T<$3T>>() {}).to($4T.class)",
      classOf[TypeLiteral[?]],
      classOf[DeserializationFunction[?]],
      publicClassName,
      loaderClassName
    )

    // Bind SerializationFunction<Public> -> Saver
    configureMethod.addStatement(
      "binder.bind(new $1T<$2T<$3T>>() {}).to($4T.class)",
      classOf[TypeLiteral[?]],
      classOf[SerializationFunction[?]],
      publicClassName,
      saverClassName
    )

    if (configStructureAnalysis.needsValidation(ast)) {
      val validatorClassName = classNameGenerator.getValidatorClassName(ast)
      configureMethod.addStatement("binder.bind($T.class)", validatorClassName)
    }

    for (enclosed <- ast.enclosed().asScala) {
      addFunctionBindings(configureMethod, enclosed)
    }

  private def addProvidesMethods(
    builder: TypeSpec.Builder,
    ast: AbstractConfigStructure,
    parent: AbstractConfigStructure,
    isParentProvided: Boolean
  ): Unit =
    val publicClassName = classNameGenerator.getPublicClassName(ast)
    var isCurrentProvided = false

    if (ast.settings().source() != null) {
      isCurrentProvided = true
      val implClassName = classNameGenerator.translateConfigClassName(ast)
      val name = publicClassName.simpleName()

      // @Provides SaveableConfigProvider<Public>
      val saveableProviderMethod = MethodSpec.methodBuilder("provide" + name + "SaveableProvider")
        .addJavadoc("Provides a {@link $T} for {@link $T}.", classOf[SaveableConfigProvider[?]], publicClassName)
        .addAnnotation(classOf[Provides])
        .addAnnotation(classOf[Singleton])
        .addAnnotation(AnnotationSpec.builder(classOf[SuppressWarnings])
          .addMember("value", "$S", "unchecked")
          .build())
        .addModifiers(Modifier.PUBLIC)
        .returns(ParameterizedTypeName.get(ClassName.get(classOf[SaveableConfigProvider[?]]), publicClassName))
        .addParameter(classOf[ConfigProviderFactory], "factory")
        .addParameter(classOf[ConfigProviderImprover], "improver")
        .addParameter(
          ParameterizedTypeName.get(ClassName.get(classOf[DeserializationFunction[?]]), publicClassName),
          "deserializer"
        )
        .addParameter(
          ParameterizedTypeName.get(ClassName.get(classOf[SerializationFunction[?]]), publicClassName),
          "serializer"
        )
        .addStatement(
          "return ($T) improver.improve(factory.createProvider($T.CONFIG, deserializer, serializer).getOrThrow())",
          ParameterizedTypeName.get(ClassName.get(classOf[SaveableConfigProvider[?]]), publicClassName),
          implClassName
        )

      builder.addMethod(saveableProviderMethod.build())

      // @Provides ConfigProvider<Public>
      val providerMethod = MethodSpec.methodBuilder(
          classNameGenerator.getProvidesProviderMethodName(name))
        .addJavadoc("Provides a {@link $T} for {@link $T}.", classOf[ConfigProvider[?]], publicClassName)
        .addAnnotation(classOf[Provides])
        .addModifiers(Modifier.PUBLIC)
        .returns(ParameterizedTypeName.get(ClassName.get(classOf[ConfigProvider[?]]), publicClassName))
        .addParameter(
          ParameterizedTypeName.get(ClassName.get(classOf[SaveableConfigProvider[?]]), publicClassName),
          "provider"
        )
        .addStatement("return provider")

      builder.addMethod(providerMethod.build())

      if (publicClassName != implClassName) {
        // @Provides SaveableConfigProvider<Impl>
        val saveableImplProviderMethod = MethodSpec.methodBuilder("provide" + name + "ImplSaveableProvider")
          .addJavadoc("Provides a {@link $T} for {@link $T}.", classOf[SaveableConfigProvider[?]], implClassName)
          .addAnnotation(classOf[Provides])
          .addAnnotation(AnnotationSpec.builder(classOf[SuppressWarnings])
            .addMember("value", "$S", "unchecked")
            .build())
          .addModifiers(Modifier.PUBLIC)
          .returns(ParameterizedTypeName.get(ClassName.get(classOf[SaveableConfigProvider[?]]), implClassName))
          .addParameter(
            ParameterizedTypeName.get(ClassName.get(classOf[SaveableConfigProvider[?]]), publicClassName),
            "provider"
          )
          .addStatement(
            "return ($T) (SaveableConfigProvider<?>) provider",
            ParameterizedTypeName.get(ClassName.get(classOf[SaveableConfigProvider[?]]), implClassName)
          )
        builder.addMethod(saveableImplProviderMethod.build())

        // @Provides ConfigProvider<Impl>
        val implProviderMethod = MethodSpec.methodBuilder("provide" + name + "ImplProvider")
          .addJavadoc("Provides a {@link $T} for {@link $T}.", classOf[ConfigProvider[?]], implClassName)
          .addAnnotation(classOf[Provides])
          .addAnnotation(AnnotationSpec.builder(classOf[SuppressWarnings])
            .addMember("value", "$S", "unchecked")
            .build())
          .addModifiers(Modifier.PUBLIC)
          .returns(ParameterizedTypeName.get(ClassName.get(classOf[ConfigProvider[?]]), implClassName))
          .addParameter(
            ParameterizedTypeName.get(ClassName.get(classOf[ConfigProvider[?]]), publicClassName),
            "provider"
          )
          .addStatement(
            "return ($T) (ConfigProvider<?>) provider",
            ParameterizedTypeName.get(ClassName.get(classOf[ConfigProvider[?]]), implClassName)
          )
        builder.addMethod(implProviderMethod.build())

        // @Provides Impl
        val implMethod = MethodSpec.methodBuilder("provide" + name + "Impl")
          .addJavadoc("Provides the {@link $T} instance.", implClassName)
          .addAnnotation(classOf[Provides])
          .addModifiers(Modifier.PUBLIC)
          .returns(implClassName)
          .addParameter(publicClassName, "config")
          .addStatement("return ($T) config", implClassName)
        builder.addMethod(implMethod.build())
      }

      // @Provides Public
      val configMethod = MethodSpec.methodBuilder(classNameGenerator.getProvidesMethodName(name))
        .addJavadoc("Provides the {@link $T} instance.", publicClassName)
        .addAnnotation(classOf[Provides])
        .addModifiers(Modifier.PUBLIC)
        .returns(publicClassName)
        .addParameter(
          ParameterizedTypeName.get(ClassName.get(classOf[ConfigProvider[?]]), publicClassName), "provider"
        )
        .addStatement("return provider.get()")

      builder.addMethod(configMethod.build())

      // Multibinder registrations

      val configMultiBinder = MethodSpec.methodBuilder(
          classNameGenerator.getProvidesToConfigSetMethodName(name))
        .addJavadoc("Adds {@link $T} to the set of all configurations.", publicClassName)
        .addAnnotation(classOf[ProvidesIntoSet])
        .addModifiers(Modifier.PUBLIC)
        .returns(ParameterizedTypeName.get(
          ClassName.get(classOf[Configuration[?]]), WildcardTypeName.subtypeOf(classOf[Object])))
        .addStatement("return $T.CONFIG", implClassName)
      builder.addMethod(configMultiBinder.build())

      val providerMultiBinder = MethodSpec.methodBuilder(
          classNameGenerator.getProvidesToProviderSetMethodName(name))
        .addJavadoc("Adds the {@link $T} for {@link $T} to the set of all providers.",
          classOf[ConfigProvider[?]], publicClassName)
        .addAnnotation(classOf[ProvidesIntoSet])
        .addModifiers(Modifier.PUBLIC)
        .returns(ParameterizedTypeName.get(
          ClassName.get(classOf[ConfigProvider[?]]), WildcardTypeName.subtypeOf(classOf[Object])))
        .addParameter(
          ParameterizedTypeName.get(ClassName.get(classOf[ConfigProvider[?]]), publicClassName), "provider"
        )
        .addStatement("return provider")
      builder.addMethod(providerMultiBinder.build())
    } else if (isParentProvided && parent != null) {
      isCurrentProvided = addNestedProvidesMethod(builder, parent, ast)
    }

    for (enclosed <- ast.enclosed().asScala) {
      addProvidesMethods(builder, enclosed, ast, isCurrentProvided)
    }

  private def addNestedProvidesMethod(
    builder: TypeSpec.Builder,
    parent: AbstractConfigStructure,
    child: AbstractConfigStructure
  ): Boolean =
    val parentPublicName = classNameGenerator.getPublicClassName(parent)
    val childPublicName = classNameGenerator.getPublicClassName(child)

    val matchingProperties = parent.properties().stream()
      .filter(property =>
        classNameGenerator.publicPropertyClassName(property) == childPublicName)
      .toList.asScala

    if (matchingProperties.isEmpty) {
      return false
    }

    var propertyToBind: Property = null
    if (matchingProperties.size == 1) {
      propertyToBind = matchingProperties.head
    } else {
      // Check for @BindProperty
      val explicitBindings = matchingProperties
        .filter(p => p.source().element().getAnnotation(classOf[BindProperty]) != null)

      if (explicitBindings.size == 1) {
        propertyToBind = explicitBindings.head
      } else {
        if (explicitBindings.size > 1) {
          for (explicitBinding <- explicitBindings) {
            MessagerUtils.error(
              explicitBinding.source().element(),
              "Multiple properties of type "
                + childPublicName.simpleName()
                + " are marked with @BindProperty. Only one can be bound to the type in Guice."
            )
          }
        }
        return false
      }
    }

    val name = childPublicName.simpleName()
    val configMethod = MethodSpec.methodBuilder(classNameGenerator.getProvidesMethodName(name))
      .addJavadoc(
        "Provides the {@link $T} instance from its parent {@link $T}.",
        childPublicName,
        parentPublicName
      )
      .addAnnotation(classOf[Provides])
      .addModifiers(Modifier.PUBLIC)
      .returns(childPublicName)
      .addParameter(parentPublicName, "parent")
      .addStatement("return parent.$L()", methodNames.safeMethodName(propertyToBind))
    builder.addMethod(configMethod.build())
    true
