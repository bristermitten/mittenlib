package me.bristermitten.mittenlib.annotations.compile

import com.google.inject.Inject
import com.palantir.javapoet.*
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.{ArrayDeque, List => JList, Optional => JOptional}
import javax.annotation.processing.Generated
import javax.lang.model.element.Modifier
import javax.lang.model.`type`.TypeMirror
import me.bristermitten.mittenlib.annotations.ast.{
  ASTSettings,
  AbstractConfigStructure,
  ConfigTypeSource,
  Property
}
import me.bristermitten.mittenlib.annotations.config.ConfigProcessor
import me.bristermitten.mittenlib.annotations.util.ConfigStructureAnalysis
import me.bristermitten.mittenlib.annotations.util.Nullity
import me.bristermitten.mittenlib.config.Configuration
import me.bristermitten.mittenlib.config.GeneratedConfig
import me.bristermitten.mittenlib.config.Source
import me.bristermitten.mittenlib.config.exception.ConfigLoadingErrors
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

class ConfigImplGenerator @Inject() (
    private val accessorGenerator: AccessorGenerator,
    private val toStringGenerator: ToStringGenerator,
    private val equalsHashCodeGenerator: EqualsHashCodeGenerator,
    private val configurationClassNameGenerator: ConfigurationClassNameGenerator,
    private val configNameCache: ConfigNameCache,
    private val methodNames: MethodNames,
    private val configStructureAnalysis: ConfigStructureAnalysis
):

  /** Generates a JavaFile containing the implementation class for the given
    * configuration structure.
    *
    * @param ast
    *   The abstract configuration structure to generate an implementation for
    * @return
    *   A JavaFile containing the generated implementation class
    */
  def emit(ast: AbstractConfigStructure): JavaFile =
    val configImplClassName =
      configurationClassNameGenerator.generateConfigurationClassName(
        ast.source().element()
      )
    val source = TypeSpec
      .classBuilder(configImplClassName)
      .addJavadoc(
        """Generated data implementation of {@link $T}.
          |""".stripMargin,
        ast.source().element()
      )

    emitInto(ast, source)

    JavaFile
      .builder(configImplClassName.packageName(), source.build())
      .skipJavaLangImports(true)
      .build()

  /** Adds all necessary elements to the {@@@@@linkTypeSpec.Builder} to create a
    * complete implementation class.
    */
  private def emitInto(
      ast: AbstractConfigStructure,
      source: TypeSpec.Builder
  ): Unit =
    val configImplClassName =
      configurationClassNameGenerator.generateConfigurationClassName(
        ast.source().element()
      )
    source.addModifiers(Modifier.PUBLIC)
    makeAbstractIfUnion(ast, source)
    addSourceElement(ast, source)
    addInheritance(ast, source)
    addInnerDefaultMethodImpl(source, ast)
    addGeneratedConfigAnnotations(ast, source)
    addNestedClassModifiers(ast, source)
    addProperties(ast, source)
    addSuperClassField(ast, source)
    accessorGenerator.createWithMethods(source, ast)
    addAllArgsConstructor(source, ast)
    addStandardObjectMethods(ast, configImplClassName, source)
    addChildClasses(ast, source)

  private def makeAbstractIfUnion(
      ast: AbstractConfigStructure,
      source: TypeSpec.Builder
  ): Unit =
    ast match {
      case _: AbstractConfigStructure.Union =>
        source.addModifiers(Modifier.ABSTRACT)
      case _ =>
    }

  /** Adds the {@@@@@codeCONFIG} static field to the class if a
    * {@@@@@linkSource} is defined.
    */
  private def addSourceElement(
      ast: AbstractConfigStructure,
      builder: TypeSpec.Builder
  ): Unit =
    if (ast.settings().source() != null) {
      val publicClassName =
        configurationClassNameGenerator.getPublicClassName(ast)
      val implementationClassName =
        configurationClassNameGenerator.translateConfigClassName(ast)

      val configFieldBuilder = FieldSpec
        .builder(
          ParameterizedTypeName
            .get(ClassName.get(classOf[Configuration[?]]), publicClassName),
          "CONFIG"
        )
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)

      configFieldBuilder.initializer(
        "new $T<>($S, $T.class, $T.class)",
        classOf[Configuration[?]],
        ast.settings().source().value(),
        publicClassName,
        implementationClassName
      )

      builder.addField(configFieldBuilder.build())
    }

  /** Adds inheritance information to the generated implementation class.
    */
  private def addInheritance(
      ast: AbstractConfigStructure,
      source: TypeSpec.Builder
  ): Unit =
    ast.source() match {
      case _: ConfigTypeSource.InterfaceConfigTypeSource =>
        source.addSuperinterface(ast.name())
      case classParent: ConfigTypeSource.ClassConfigTypeSource =>
        for {
          parentType <- classParent.parent().toScala
          parentAst <- configNameCache.lookupAST(parentType).toScala
        } {
          source.superclass(
            configurationClassNameGenerator.translateConfigClassName(parentAst)
          )
        }
    }

  /** Adds {@@@@@linkGeneratedConfig} and {@@@@@linkGenerated} annotations to
    * the class.
    */
  private def addGeneratedConfigAnnotations(
      ast: AbstractConfigStructure,
      source: TypeSpec.Builder
  ): Unit =
    val uninitializableProperties = ast
      .properties()
      .asScala
      .filter(p =>
        !p.settings().hasDefaultValue
          && !p.settings().isNullable
          && !configStructureAnalysis.isTypeInitializable(p.propertyType())
      )
      .map(_.name())
      .toList

    val generatedConfigBuilder = AnnotationSpec
      .builder(classOf[GeneratedConfig])
      .addMember("source", "$T.class", ast.name())
      .addMember(
        "isDynamicallyInitializable",
        "$L",
        Boolean.box(configStructureAnalysis.isDynamicallyInitializable(ast))
      )

    for (property <- uninitializableProperties) {
      generatedConfigBuilder.addMember(
        "uninitializableProperties",
        "$S",
        property
      )
    }

    source.addAnnotation(generatedConfigBuilder.build())

    source.addAnnotation(GeneratorUtil.generatedAnnotation(true))

  /** Ensures nested classes are marked as {@@@@@codestatic} .
    */
  private def addNestedClassModifiers(
      ast: AbstractConfigStructure,
      source: TypeSpec.Builder
  ): Unit =
    if (ast.enclosedIn() != null) {
      source.addModifiers(Modifier.STATIC)
    }

  /** Adds all properties as fields and accessors to the implementation class.
    */
  private def addProperties(
      ast: AbstractConfigStructure,
      source: TypeSpec.Builder
  ): Unit =
    for (property <- ast.properties().asScala) {
      addProperty(property, source)
    }

  /** Adds {@@@@@codeequals} and {@@@@@codehashCode} methods, plus
    * {@@@@@codetoString} if
    * {@@@@linkASTSettings.ConfigASTSettings#generateToString()} is true
    */
  private def addStandardObjectMethods(
      ast: AbstractConfigStructure,
      configImplClassName: ClassName,
      source: TypeSpec.Builder
  ): Unit =
    if (ast.settings().generateToString()) {
      val toString = toStringGenerator.generateToString(
        ast.properties(),
        configImplClassName
      )
      source.addMethod(toString)
    }

    source.addMethod(
      equalsHashCodeGenerator
        .generateEquals(configImplClassName, ast.properties())
    )
    source.addMethod(equalsHashCodeGenerator.generateHashCode(ast.properties()))

  /** Recursively adds implementation classes for enclosed configuration
    * structures.
    */
  private def addChildClasses(
      ast: AbstractConfigStructure,
      source: TypeSpec.Builder
  ): Unit =
    for (child <- ast.enclosed().asScala) {
      val childClassName =
        configurationClassNameGenerator.translateConfigClassName(child)
      val childBuilder = TypeSpec.classBuilder(childClassName)
      emitInto(child, childBuilder)
      source.addType(childBuilder.build())
    }

  /** Adds a single property as a private final field and its corresponding
    * getter.
    */
  private def addProperty(property: Property, source: TypeSpec.Builder): Unit =
    val field = FieldSpec
      .builder(
        configurationClassNameGenerator
          .publicPropertyClassName(property)
          .annotated(Nullity.getNullityAnnotationSpec(property)),
        property.name(),
        Modifier.FINAL,
        Modifier.PRIVATE
      )
      .build()

    source.addField(field)

    property.source() match {
      case fieldSource: Property.PropertySource.FieldSource =>
        accessorGenerator.createGetterMethod(
          source,
          fieldSource.element(),
          field
        )
      case methodSource: Property.PropertySource.MethodSource =>
        accessorGenerator.createGetterMethodOverriding(
          source,
          methodSource.element(),
          field
        )
    }

  private def getSuperClass(ast: AbstractConfigStructure): Option[TypeMirror] =
    ast.source() match {
      case classParent: ConfigTypeSource.ClassConfigTypeSource =>
        classParent.parent().toScala
      case _ =>
        None
    }

  private def getSuperClass(tpe: TypeMirror): Option[TypeMirror] =
    configNameCache.lookupAST(tpe).toScala.flatMap(getSuperClass)

  /** Adds an all-argument constructor to the implementation class.
    */
  private def addAllArgsConstructor(
      source: TypeSpec.Builder,
      ast: AbstractConfigStructure
  ): Unit =
    val constructor = MethodSpec
      .constructorBuilder()
      .addJavadoc(
        "Constructs a new implementation instance with all properties populated.\n"
      )
      .addModifiers(Modifier.PUBLIC)

    addSuperClassParameter(ast, constructor)
    addPropertyParameters(ast, constructor)

    source.addMethod(constructor.build())

  /** Adds a parameter to the constructor for the parent configuration class, if
    * applicable.
    */
  private def addSuperClassParameter(
      ast: AbstractConfigStructure,
      constructor: MethodSpec.Builder
  ): Unit =
    for {
      parent <- getSuperClass(ast)
      parentConfig <- configNameCache
        .lookupAST(parent)
        .toScala
        .orElse(
          throw new IllegalStateException(
            "could not determine a config for parent class " + parent
          )
        )
    } {
      val parentName =
        configurationClassNameGenerator.translateConfigClassName(parentConfig)
      val superParameterName = "parent"
      constructor.addParameter(
        ParameterSpec
          .builder(parentName, superParameterName, Modifier.FINAL)
          .build()
      )

      val parentParams =
        buildSuperConstructorParams(parent, parentConfig, superParameterName)
      constructor.addStatement("super($L)", parentParams.mkString(", "))
      constructor.addStatement("this.parent = parent")
    }

  /** Adds a field to the implementation class to store the parent configuration
    * instance.
    */
  private def addSuperClassField(
      ast: AbstractConfigStructure,
      builder: TypeSpec.Builder
  ): Unit =
    for {
      parent <- getSuperClass(ast)
      parentConfig <- configNameCache
        .lookupAST(parent)
        .toScala
        .orElse(
          throw new IllegalStateException(
            "could not determine a config for parent class " + parent
          )
        )
    } {
      val parentName =
        configurationClassNameGenerator.translateConfigClassName(parentConfig)
      val field = FieldSpec.builder(
        parentName,
        "parent",
        Modifier.PRIVATE,
        Modifier.FINAL
      )
      builder.addField(field.build())
    }

  /** Builds the list of parameters to be passed to the super constructor.
    */
  private def buildSuperConstructorParams(
      parent: TypeMirror,
      parentConfig: AbstractConfigStructure,
      superParameterName: String
  ): List[String] =
    val parentParams = parentConfig
      .properties()
      .asScala
      .map(variableElement =>
        superParameterName + "." + methodNames.safeMethodName(
          variableElement
        ) + "()"
      )
      .toList

    getSuperClass(parent) match {
      case Some(_) =>
        superParameterName :: parentParams
      case None =>
        parentParams
    }

  /** Adds constructor parameters and initialization statements for all
    * properties.
    */
  private def addPropertyParameters(
      ast: AbstractConfigStructure,
      constructor: MethodSpec.Builder
  ): Unit =
    for (property <- ast.properties().asScala) {
      val parameter = createPropertyParameter(property)
      constructor.addParameter(parameter)
      constructor.addStatement("this.$N = $N", property.name(), property.name())
    }

  private def createPropertyParameter(property: Property): ParameterSpec =
    val nullityAnnotation = Nullity.getNullityAnnotation(property)
    val builder = ParameterSpec
      .builder(
        configurationClassNameGenerator
          .publicPropertyClassName(property)
          .annotated(AnnotationSpec.builder(nullityAnnotation).build()),
        property.name()
      )
      .addModifiers(Modifier.FINAL)

    builder.build()

  /** Create a dummy class/interface named "DefaultMethodAccess".
    */
  private def addInnerDefaultMethodImpl(
      typeSpecBuilder: TypeSpec.Builder,
      ast: AbstractConfigStructure
  ): JOptional[ClassName] =
    ast.source() match {
      case _: ConfigTypeSource.InterfaceConfigTypeSource =>
        val hasAnyDefaultValue =
          ast.properties().asScala.exists(_.settings().hasDefaultValue())
        if (!hasAnyDefaultValue) {
          JOptional.empty()
        } else {
          val concreteConfigClassName =
            configurationClassNameGenerator.getConcreteConfigClassName(ast)
          val innerName =
            configurationClassNameGenerator.getDefaultMethodAccessClassName(ast)

          val innerBuilder = TypeSpec.classBuilder(innerName)
          innerBuilder.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
          innerBuilder.addSuperinterface(ast.name())

          for (
            property <- ast.properties().asScala
            if !property.settings().hasDefaultValue()
          ) {
            innerBuilder.addMethod(
              MethodSpec
                .methodBuilder(property.name())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(classOf[Override])
                .returns(
                  configurationClassNameGenerator
                    .publicPropertyClassName(property)
                )
                .addStatement(
                  "throw $T.defaultValueProxyException($T.class, $S)",
                  classOf[ConfigLoadingErrors],
                  concreteConfigClassName,
                  property.name()
                )
                .build()
            )
          }

          typeSpecBuilder.addType(innerBuilder.build())
          JOptional.of(innerName)
        }
      case _ =>
        JOptional.empty()
    }
