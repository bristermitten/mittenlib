package me.bristermitten.mittenlib.annotations.compile

import com.google.inject.Inject
import com.palantir.javapoet.*
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.{ArrayDeque, List => JList, Optional => JOptional}
import javax.annotation.processing.Generated
import javax.lang.model.element.{Modifier, TypeElement, Element}
import javax.lang.model.`type`.TypeMirror
import me.bristermitten.mittenlib.annotations.domain.*
import me.bristermitten.mittenlib.annotations.ast.AbstractConfigStructure
import me.bristermitten.mittenlib.annotations.config.ConfigProcessor
import me.bristermitten.mittenlib.annotations.util.ConfigStructureAnalysis
import me.bristermitten.mittenlib.annotations.util.ElementsFinder
import me.bristermitten.mittenlib.config.Configuration
import me.bristermitten.mittenlib.config.GeneratedConfig
import me.bristermitten.mittenlib.config.Source
import me.bristermitten.mittenlib.config.exception.ConfigLoadingErrors
import javax.annotation.processing.ProcessingEnvironment
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

class ConfigImplGenerator @Inject() (
    private val accessorGenerator: AccessorGenerator,
    private val toStringGenerator: ToStringGenerator,
    private val equalsHashCodeGenerator: EqualsHashCodeGenerator,
    private val configurationClassNameGenerator: ConfigurationClassNameGenerator,
    private val configNameCache: ConfigNameCache,
    private val methodNames: MethodNames,
    private val configStructureAnalysis: ConfigStructureAnalysis,
    private val elementsFinder: ElementsFinder,
    processingEnv: ProcessingEnvironment
):
  private val elements = processingEnv.getElementUtils

  /** Generates a JavaFile containing the implementation class for the given
    * configuration structure.
    *
    * @param ast
    *   The abstract configuration structure to generate an implementation for
    * @return
    *   A JavaFile containing the generated implementation class
    */
  def emit(ast: ConfigStructure): JavaFile =
    val dtoType = elements.getTypeElement(ast.name.canonicalName())
    val configImplClassName =
      configurationClassNameGenerator.generateConfigurationClassName(dtoType)
    val source = TypeSpec
      .classBuilder(configImplClassName)
      .addJavadoc(
        """Generated data implementation of {@link $T}.
          |""".stripMargin,
        dtoType
      )

    emitInto(ast, source)

    JavaFile
      .builder(configImplClassName.packageName(), source.build())
      .skipJavaLangImports(true)
      .build()

  /** Adds all necessary elements to the {@link TypeSpec.Builder} to create a
    * complete implementation class.
    */
  private def emitInto(
      ast: ConfigStructure,
      source: TypeSpec.Builder
  ): Unit =
    val dtoType = elements.getTypeElement(ast.name.canonicalName())
    val configImplClassName =
      configurationClassNameGenerator.generateConfigurationClassName(dtoType)
    source.addModifiers(Modifier.PUBLIC)
    makeAbstractIfUnion(ast, source)
    addSourceElement(ast, source)
    addInheritance(ast, source)
    addInnerDefaultMethodImpl(source, ast)
    addGeneratedConfigAnnotations(ast, source)
    addNestedClassModifiers(ast, source)
    addProperties(ast, source)
    addSuperClassField(ast, source)
    configNameCache
      .lookupAST(ast.name)
      .toScala
      .foreach(rawAst => accessorGenerator.createWithMethods(source, rawAst))
    addAllArgsConstructor(source, ast)
    if (
      configStructureAnalysis.isDynamicallyInitializable(ast) && !ast
        .isInstanceOf[ConfigStructure.Union]
    ) {
      if (ast.properties.nonEmpty || getParentConfig(ast).isDefined) {
        addNoArgsConstructor(source, ast)
      }
    }
    addStandardObjectMethods(ast, configImplClassName, source)
    addChildClasses(ast, source)

  private def makeAbstractIfUnion(
      ast: ConfigStructure,
      source: TypeSpec.Builder
  ): Unit =
    ast match {
      case _: ConfigStructure.Union =>
        source.addModifiers(Modifier.ABSTRACT)
      case _ =>
    }

  /** Adds the {@code CONFIG} static field to the class if a {@link Source} is
    * defined.
    */
  private def addSourceElement(
      ast: ConfigStructure,
      builder: TypeSpec.Builder
  ): Unit =
    ast.settings.source.foreach { sourceVal =>
      val dtoType = elements.getTypeElement(ast.name.canonicalName())
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
        sourceVal,
        publicClassName,
        implementationClassName
      )

      builder.addField(configFieldBuilder.build())
    }

  /** Adds inheritance information to the generated implementation class.
    */
  private def addInheritance(
      ast: ConfigStructure,
      source: TypeSpec.Builder
  ): Unit =
    ast match {
      case ConfigStructure.Atomic(name, isInterface, parentClass, _, _, _) =>
        if (isInterface) {
          source.addSuperinterface(name)
        } else {
          for {
            parentType <- parentClass
            parentAst <- configNameCache.lookupAST(parentType).toScala
          } {
            source.superclass(
              configurationClassNameGenerator.translateConfigClassName(
                parentAst
              )
            )
          }
        }
      case ConfigStructure.Intersection(name, _, _, _, _) =>
        source.addSuperinterface(name)
      case ConfigStructure.Union(name, _, _, _, _) =>
        source.addSuperinterface(name)
    }

  /** Adds {@link GeneratedConfig} and {@link Generated} annotations to the
    * class.
    */
  private def addGeneratedConfigAnnotations(
      ast: ConfigStructure,
      source: TypeSpec.Builder
  ): Unit =
    val uninitializableProperties = ast.properties
      .filter(p =>
        !p.hasDefault
          && !p.isNullable
          && !configStructureAnalysis.isTypeInitializable(p.propertyType)
      )
      .map(_.name)

    val generatedConfigBuilder = AnnotationSpec
      .builder(classOf[GeneratedConfig])
      .addMember("source", "$T.class", ast.name)
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

  /** Ensures nested classes are marked as {@code static} .
    */
  private def addNestedClassModifiers(
      ast: ConfigStructure,
      source: TypeSpec.Builder
  ): Unit =
    if (ast.name.enclosingClassName() != null) {
      source.addModifiers(Modifier.STATIC)
    }

  /** Adds all properties as fields and accessors to the implementation class.
    */
  private def addProperties(
      ast: ConfigStructure,
      source: TypeSpec.Builder
  ): Unit =
    val dtoType = elements.getTypeElement(ast.name.canonicalName())
    for (property <- ast.properties) {
      addProperty(ast, property, dtoType, source)
    }

  /** Adds {@code equals} and {@code hashCode} methods, and {@code toString}
    */
  private def addStandardObjectMethods(
      ast: ConfigStructure,
      configImplClassName: ClassName,
      source: TypeSpec.Builder
  ): Unit =
    val propertiesJavaList =
      ast.properties.asJava // For toString/equals/hashCode generators
    // toString, equals, hashCode are always generated now
    val toString = toStringGenerator.generateToStringDomain(
      propertiesJavaList,
      configImplClassName
    )
    source.addMethod(toString)

    source.addMethod(
      equalsHashCodeGenerator.generateEqualsDomain(
        configImplClassName,
        propertiesJavaList
      )
    )
    source.addMethod(
      equalsHashCodeGenerator.generateHashCodeDomain(propertiesJavaList)
    )

  /** Recursively adds implementation classes for enclosed configuration
    * structures.
    */
  private def addChildClasses(
      ast: ConfigStructure,
      source: TypeSpec.Builder
  ): Unit =
    for (child <- ast.enclosed) {
      val childClassName =
        configurationClassNameGenerator.translateConfigClassName(child)
      val childBuilder = TypeSpec.classBuilder(childClassName)
      emitInto(child, childBuilder)
      source.addType(childBuilder.build())
    }

  private def getAnnotatedPropertyType(property: Property): TypeName =
    val nullityAnnotation =
      if (property.isNullable) classOf[org.jspecify.annotations.Nullable]
      else classOf[org.jspecify.annotations.NonNull]
    configurationClassNameGenerator
      .publicPropertyClassName(property.typeMirror)
      .annotated(AnnotationSpec.builder(nullityAnnotation).build())

  /** Adds a single property as a private final field and its corresponding
    * getter.
    */
  private def addProperty(
      ast: ConfigStructure,
      property: Property,
      dtoType: TypeElement,
      source: TypeSpec.Builder
  ): Unit =
    val field = FieldSpec
      .builder(
        getAnnotatedPropertyType(property),
        property.name,
        Modifier.FINAL,
        Modifier.PRIVATE
      )
      .build()

    source.addField(field)

    val isInterface = ast match {
      case ConfigStructure.Atomic(_, isInterface, _, _, _, _) => isInterface
      case _                                                  => true
    }

    if (isInterface) {
      val methodOpt = elementsFinder
        .getPropertyMethods(dtoType)
        .find(_.getSimpleName.toString == property.name)
      methodOpt.foreach { method =>
        accessorGenerator.createGetterMethodOverriding(source, method, field)
      }
    } else {
      val fieldOpt = elementsFinder
        .getApplicableVariableElements(dtoType)
        .find(_.getSimpleName.toString == property.name)
      fieldOpt.foreach { f =>
        accessorGenerator.createGetterMethod(source, f, field)
      }
    }

  private def getSuperClass(ast: ConfigStructure): Option[ClassName] =
    ast match {
      case ConfigStructure.Atomic(_, _, parentClass, _, _, _) =>
        parentClass
      case _ =>
        None
    }

  /** Adds an all-argument constructor to the implementation class.
    */
  private def addAllArgsConstructor(
      source: TypeSpec.Builder,
      ast: ConfigStructure
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

  private def getParentConfig(
      ast: ConfigStructure
  ): Option[(ClassName, AbstractConfigStructure)] =
    getSuperClass(ast).map { parent =>
      val parentConfig = configNameCache
        .lookupAST(parent)
        .toScala
        .getOrElse(
          throw new IllegalStateException(
            "could not determine a config for parent class " + parent
          )
        )
      (parent, parentConfig)
    }

  /** Adds a parameter to the constructor for the parent configuration class, if
    * applicable.
    */
  private def addSuperClassParameter(
      ast: ConfigStructure,
      constructor: MethodSpec.Builder
  ): Unit =
    getParentConfig(ast).foreach { case (parent, parentConfig) =>
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
      ast: ConfigStructure,
      builder: TypeSpec.Builder
  ): Unit =
    getParentConfig(ast).foreach { case (_, parentConfig) =>
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
      parent: ClassName,
      parentConfig: AbstractConfigStructure,
      superParameterName: String
  ): List[String] =
    val parentParams = parentConfig
      .properties()
      .asScala
      .toList
      .map(p => superParameterName + "." + methodNames.safeMethodName(p) + "()")

    val grandParentExists = parentConfig.source() match {
      case c: me.bristermitten.mittenlib.annotations.ast.ConfigTypeSource.ClassConfigTypeSource =>
        c.parentField.isPresent
      case _ => false
    }

    if (grandParentExists) {
      superParameterName :: parentParams
    } else {
      parentParams
    }

  /** Adds constructor parameters and initialization statements for all
    * properties.
    */
  private def addPropertyParameters(
      ast: ConfigStructure,
      constructor: MethodSpec.Builder
  ): Unit =
    for (property <- ast.properties) {
      val parameter = createPropertyParameter(property)
      constructor.addParameter(parameter)
      constructor.addStatement("this.$N = $N", property.name, property.name)
    }

  private def createPropertyParameter(property: Property): ParameterSpec =
    val builder = ParameterSpec
      .builder(
        getAnnotatedPropertyType(property),
        property.name
      )
      .addModifiers(Modifier.FINAL)

    builder.build()

  private def addNoArgsConstructor(
      source: TypeSpec.Builder,
      ast: ConfigStructure
  ): Unit =
    val constructor = MethodSpec
      .constructorBuilder()
      .addJavadoc(
        "Constructs a new implementation instance with default values.\n"
      )
      .addModifiers(Modifier.PUBLIC)

    val parentOpt = getParentConfig(ast)
    if (parentOpt.isDefined) {
      constructor.addStatement("super()")
      constructor.addStatement("this.parent = null")
    }

    val hasAnyDefaultValue =
      ast.properties.exists(configStructureAnalysis.hasDefaultOrIsInitializable)
    if (
      hasAnyDefaultValue && (ast match {
        case atomic: ConfigStructure.Atomic  => atomic.isInterface
        case _: ConfigStructure.Intersection => true
        case _: ConfigStructure.Union        => false
      })
    ) {
      val innerName =
        configurationClassNameGenerator.getDefaultMethodAccessClassName(ast)
      constructor.addStatement(
        "$T defaultAccess = new $T()",
        innerName,
        innerName
      )
      for (property <- ast.properties) {
        constructor.addStatement(
          "this.$N = defaultAccess.$N()",
          property.name,
          property.name
        )
      }
    } else {
      for (property <- ast.properties) {
        val typeName = configurationClassNameGenerator.publicPropertyClassName(
          property.typeMirror
        )
        if (typeName.isPrimitive) {
          if (typeName == TypeName.BOOLEAN) {
            constructor.addStatement("this.$N = false", property.name)
          } else {
            constructor.addStatement("this.$N = 0", property.name)
          }
        } else {
          constructor.addStatement("this.$N = null", property.name)
        }
      }
    }

    source.addMethod(constructor.build())

  /** Create a dummy class/interface named "DefaultMethodAccess".
    */
  private def addInnerDefaultMethodImpl(
      typeSpecBuilder: TypeSpec.Builder,
      ast: ConfigStructure
  ): JOptional[ClassName] =
    val isInterfaceWithDefaults = ast match {
      case ConfigStructure.Atomic(_, true, _, _, properties, _) =>
        Some(properties)
      case ConfigStructure.Intersection(_, _, _, properties, _) =>
        Some(properties)
      case _ => None
    }
    isInterfaceWithDefaults match {
      case None             => JOptional.empty()
      case Some(properties) =>
        val hasAnyDefaultValue =
          properties.exists(configStructureAnalysis.hasDefaultOrIsInitializable)
        if (!hasAnyDefaultValue) {
          JOptional.empty()
        } else {
          val concreteConfigClassName =
            configurationClassNameGenerator.getConcreteConfigClassName(ast)
          val innerName =
            configurationClassNameGenerator.getDefaultMethodAccessClassName(ast)

          val innerBuilder = TypeSpec.classBuilder(innerName)
          innerBuilder.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
          innerBuilder.addSuperinterface(ast.name)

          for (
            property <- properties
            if !property.hasDefault
          ) {
            val methodBuilder = MethodSpec
              .methodBuilder(property.name)
              .addModifiers(Modifier.PUBLIC)
              .addAnnotation(classOf[Override])
              .returns(
                configurationClassNameGenerator
                  .publicPropertyClassName(property.typeMirror)
              )

            if (
              configStructureAnalysis.isTypeInitializable(property.propertyType)
            ) {
              property.propertyType match {
                case PropertyType.ConfigProperty(className, _) =>
                  val concreteType =
                    configurationClassNameGenerator.translateConfigClassName(
                      className
                    )
                  methodBuilder.addStatement("return new $T()", concreteType)
                case PropertyType.OptionalProperty(_) =>
                  methodBuilder.addStatement(
                    "return $T.empty()",
                    classOf[java.util.Optional[?]]
                  )
                case other =>
                  throw new IllegalStateException(
                    s"Type marked initializable but has no default generator: $other"
                  )
              }
            } else if (property.isNullable) {
              methodBuilder.addStatement("return null")
            } else {
              methodBuilder.addStatement(
                "throw $T.defaultValueProxyException($T.class, $S)",
                classOf[ConfigLoadingErrors],
                concreteConfigClassName,
                property.name
              )
            }
            innerBuilder.addMethod(methodBuilder.build())
          }

          typeSpecBuilder.addType(innerBuilder.build())
          JOptional.of(innerName)
        }
    }
