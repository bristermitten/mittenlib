package me.bristermitten.mittenlib.annotations.parser

import cats.data.ValidatedNel
import cats.implicits.*
import com.google.inject.Inject
import com.palantir.javapoet.{ClassName, TypeName}
import com.sun.source.tree.*
import com.sun.source.util.Trees
import io.toolisticon.aptk.tools.TypeMirrorWrapper
import io.toolisticon.aptk.tools.corematcher.AptkCoreMatchers
import io.toolisticon.aptk.tools.wrapper.TypeElementWrapper
import me.bristermitten.mittenlib.annotations.domain.*
import me.bristermitten.mittenlib.annotations.{ast => astpkg}
import me.bristermitten.mittenlib.annotations.ast.{
  AbstractConfigStructure,
  ASTSettings,
  ASTParentReference,
  ConfigTypeSource,
  Property => ASTProperty,
  ValidationConstraint
}
import me.bristermitten.mittenlib.annotations.compile.ConfigNameCache
import me.bristermitten.mittenlib.annotations.compile.ConfigurationClassNameGenerator
import me.bristermitten.mittenlib.annotations.compile.GeneratedTypeCache
import me.bristermitten.mittenlib.annotations.util.ElementsFinder
import me.bristermitten.mittenlib.annotations.util.TypesUtil
import me.bristermitten.mittenlib.config.*
import me.bristermitten.mittenlib.config.names.ConfigName
import me.bristermitten.mittenlib.config.names.NamingPattern
import me.bristermitten.mittenlib.config.validation.Validator
import org.jspecify.annotations.Nullable

import java.lang.annotation.Annotation
import java.util.{Optional, Collections as JCollections, List as JList}
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.AnnotatedConstruct
import javax.lang.model.element.*
import javax.lang.model.`type`.{
  DeclaredType,
  PrimitiveType,
  TypeKind,
  TypeMirror
}
import io.toolisticon.aptk.tools.TypeMirrorWrapper
import io.toolisticon.aptk.tools.MessagerUtils

import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*
import me.bristermitten.mittenlib.annotations.compile.SerializationCodeGenerator
import me.bristermitten.mittenlib.annotations.util.ConfigStructureAnalysis

case class ParserError(element: Element, message: String)

class ConfigParser @Inject() (
    private val typesUtil: TypesUtil,
    private val elementsFinder: ElementsFinder,
    private val configNameCache: ConfigNameCache,
    private val generatedTypeCache: GeneratedTypeCache,
    private val classNameGenerator: ConfigurationClassNameGenerator,
    private val serializationCodeGenerator: SerializationCodeGenerator,
    private val configStructureAnalysis: ConfigStructureAnalysis,
    processingEnv: ProcessingEnvironment
):
  io.toolisticon.aptk.common.ToolingProvider.setTooling(processingEnv)

  private val trees: Option[Trees] = try {
    Option(Trees.instance(processingEnv))
  } catch {
    case _: IllegalArgumentException => None
  }
  private val elements = processingEnv.getElementUtils
  private val types = processingEnv.getTypeUtils

  def parseJava(element: TypeElement): JList[ParserError] = {
    parse(element) match {
      case Left(errors) => errors.asJava
      case Right(_)     => JCollections.emptyList()
    }
  }

  def getParsedStructure(element: TypeElement): ConfigStructure = {
    parse(element) match {
      case Right(structure) => structure
      case Left(errors)     =>
        throw new IllegalArgumentException(
          s"Invalid config: ${errors.map(_.message).mkString(", ")}"
        )
    }
  }

  def parse(
      element: TypeElement
  ): Either[List[ParserError], ConfigStructure] = {
    parseAbstract(element).toEither.leftMap(_.toList)
  }

  def parseAbstract(
      element: TypeElement
  ): ValidatedNel[ParserError, ConfigStructure] = {
    parseAbstract(element, None)
  }

  private def parseAbstract(
      element: TypeElement,
      parentPath: Option[List[ClassName]]
  ): ValidatedNel[ParserError, ConfigStructure] = {
    if (element.getKind == ElementKind.ENUM) {
      return ParserError(
        element,
        s"Enums cannot be annotated with @Config"
      ).invalidNel
    }
    val wrapper = TypeElementWrapper.wrap(element)
    val name = ClassName.get(element)

    // Enclosed configs
    val enclosedConfigsV = wrapper
      .filterEnclosedElements()
      .applyFilter(AptkCoreMatchers.IS_TYPE_ELEMENT)
      .applyFilter(AptkCoreMatchers.BY_ELEMENT_KIND)
      .filterByOneOf(ElementKind.CLASS, ElementKind.INTERFACE)
      .getResult
      .asScala
      .filter(e => typesUtil.getAnnotation(e, classOf[Config]) != null)
      .map(e =>
        parseAbstract(
          e,
          Some(parentPath.getOrElse(Nil) :+ name)
        )
      )
      .toList
      .sequence

    // Naming pattern
    val namingPattern = Option(
      typesUtil.getAnnotation(element, classOf[NamingPattern])
    )

    // Properties
    val propertiesV = getPropertiesIn(element, namingPattern)

    // Settings
    val settings = getSettings(element)

    // Parents/Superclasses
    val parentMirrors =
      (element.getSuperclass :: element.getInterfaces.asScala.toList)
        .filter(_.getKind != TypeKind.NONE)

    val parents = parentMirrors
      .map(TypeMirrorWrapper.wrap)
      .flatMap(_.getTypeElement.toScala)
      .map(_.unwrap())
      .map(ClassName.get)
      .filter(_ != ClassName.OBJECT)

    val structureV = if (wrapper.hasAnnotation(classOf[ConfigUnion])) {
      (enclosedConfigsV, propertiesV)
        .mapN { (enclosed, properties) =>
          if (properties.nonEmpty) {
            val invalidAlternatives =
              enclosed
                .filter(alt => !configStructureParents(alt).contains(name))
            if (invalidAlternatives.nonEmpty) {
              val errors = invalidAlternatives.map { alt =>
                val altElement =
                  elements.getTypeElement(alt.name.canonicalName())
                ParserError(
                  altElement,
                  s"Alternative in union ${name.canonicalName()} MUST extend the union type when the union type has properties defined!"
                )
              }
              cats.data.Validated.invalid(errors.toNel.get)
            } else {
              cats.data.Validated.valid(
                ConfigStructure
                  .Union(name, parents, settings, enclosed, properties)
              )
            }
          } else {
            cats.data.Validated.valid(
              ConfigStructure
                .Union(name, parents, settings, enclosed, properties)
            )
          }
        }
        .andThen(identity)
    } else if (parents.isEmpty) {
      (enclosedConfigsV, propertiesV).mapN { (enclosed, properties) =>
        ConfigStructure.Atomic(
          name,
          wrapper.isInterface,
          None,
          settings,
          properties,
          enclosed
        )
      }
    } else {
      if (wrapper.isClass) {
        if (parents.size > 1) {
          ParserError(
            element,
            s"Class ${element.getSimpleName} cannot extend more than one parent"
          ).invalidNel
        } else {
          (enclosedConfigsV, propertiesV).mapN { (enclosed, properties) =>
            ConfigStructure.Atomic(
              name,
              wrapper.isInterface,
              Some(parents.head),
              settings,
              properties,
              enclosed
            )
          }
        }
      } else {
        (enclosedConfigsV, propertiesV).mapN { (enclosed, properties) =>
          ConfigStructure.Intersection(
            name,
            parents,
            settings,
            properties,
            enclosed
          )
        }
      }
    }

    structureV.andThen { structure =>
      val properties = structure.properties
      val c1 = verifyZeroArgConstructor(element, properties)
      val c2 = verifySerializationRequirement(element, structure)
      val c3 = verifyDynamicInitializationRequirement(element, structure)

      (c1, c2, c3).mapN { (_, _, _) =>
        putInCache(structure, element)
        structure
      }
    }
  }

  private def verifyZeroArgConstructor(
      element: TypeElement,
      properties: List[Property]
  ): ValidatedNel[ParserError, Unit] = {
    val hasAnyDefault = properties.exists(_.hasDefault)
    if (hasAnyDefault && element.getKind == ElementKind.CLASS) {
      val noArgConstructor = element.getEnclosedElements.asScala
        .filter(_.getKind == ElementKind.CONSTRUCTOR)
        .map(_.asInstanceOf[ExecutableElement])
        .find(_.getParameters.isEmpty)

      noArgConstructor match {
        case None =>
          ParserError(
            element,
            s"Class ${element.getSimpleName} has fields with default values, but is missing an accessible (non-private) zero-arguments constructor"
          ).invalidNel
        case Some(c) if c.getModifiers.contains(Modifier.PRIVATE) =>
          ParserError(
            element,
            s"Class ${element.getSimpleName} has fields with default values, but is missing an accessible (non-private) zero-arguments constructor"
          ).invalidNel
        case _ => ().validNel
      }
    } else {
      ().validNel
    }
  }

  private def verifySerializationRequirement(
      element: TypeElement,
      structure: ConfigStructure
  ): ValidatedNel[ParserError, Unit] = {
    if (!serializationCodeGenerator.isSerializationSupported(structure)) {
      val unsupported =
        serializationCodeGenerator.getUnsupportedSerializationProperties(
          structure
        )
      val unsupportedStr = java.lang.String.join(", ", unsupported)
      if (structure.settings.requireSerialization) {
        ParserError(
          element,
          s"Serialization is required for this config, but it contains properties that cannot be serialized: $unsupportedStr"
        ).invalidNel
      } else {
        MessagerUtils.warning(
          element,
          ConfigVerificationErrors.SERIALIZATION_NOT_SUPPORTED_WARNING,
          unsupportedStr
        )
        ().validNel
      }
    } else {
      ().validNel
    }
  }

  private def verifyDynamicInitializationRequirement(
      element: TypeElement,
      structure: ConfigStructure
  ): ValidatedNel[ParserError, Unit] = {
    if (
      structure.settings.source.isDefined && !configStructureAnalysis
        .isDynamicallyInitializable(structure)
    ) {
      val missingDefaults = structure.properties
        .filter(p =>
          !p.hasDefault && !p.isNullable && !configStructureAnalysis
            .isTypeInitializable(p.propertyType)
        )
        .map(_.name)

      if (missingDefaults.nonEmpty) {
        val missingDefaultsStr = missingDefaults.mkString(", ")
        val sourceVal = structure.settings.source.get
        if (structure.settings.requireDynamicInitialization) {
          ParserError(
            element,
            s"Config ${structure.name.simpleName()} has a @Source but is not dynamically initializable because the following required properties lack default values: $missingDefaultsStr. " +
              s"You must provide a default configuration file (e.g. $sourceVal) in your jar's resources, " +
              s"or provide default values for these properties to avoid runtime errors. If you understand the risks but do not want to change the type, set requireDynamicInitialization to false to set this to a warning rather than error."
          ).invalidNel
        } else {
          MessagerUtils.warning(
            element,
            ConfigVerificationErrors.NOT_DYNAMICALLY_INITIALIZABLE,
            structure.name.simpleName(),
            missingDefaultsStr,
            sourceVal
          )
          ().validNel
        }
      } else {
        ().validNel
      }
    } else {
      ().validNel
    }
  }

  private def getPropertiesIn(
      element: TypeElement,
      namingPattern: Option[NamingPattern]
  ): ValidatedNel[ParserError, List[Property]] = {
    val wrapper = TypeElementWrapper.wrap(element)
    val elementsList = if (wrapper.isClass) {
      elementsFinder.getApplicableVariableElements(element)
    } else if (wrapper.isInterface) {
      elementsFinder.getPropertyMethods(element)
    } else {
      Nil
    }

    elementsList.map { propertyElement =>
      val propertyName = propertyElement.getSimpleName.toString
      val propertyTypeMirror = TypesUtil.getType(propertyElement)

      val configName = Option(
        typesUtil.getAnnotation(propertyElement, classOf[ConfigName])
      ).map(_.value())
      val namingPatternSub = Option(
        typesUtil.getAnnotation(propertyElement, classOf[NamingPattern])
      ).orElse(namingPattern)
      val isNullable = typesUtil.isNullable(propertyElement)

      val enumParsingScheme = Option(
        typesUtil.getAnnotation(propertyElement, classOf[EnumParsingScheme])
      )
      if (
        enumParsingScheme.isDefined &&
        propertyElement.getAnnotation(classOf[EnumParsingScheme]) != null &&
        !TypeMirrorWrapper.wrap(propertyTypeMirror).isEnum
      ) {
        MessagerUtils.warning(
          propertyElement,
          ConfigVerificationErrors.ENUM_PARSING_SCHEME_NOT_ENUM
        )
      }

      val hasDefault = propertyElement match {
        case m: ExecutableElement => m.isDefault
        case f: VariableElement   =>
          trees
            .flatMap { t =>
              Option(t.getPath(f)).map { path =>
                val tree = path.getLeaf.asInstanceOf[VariableTree]
                tree.getInitializer != null
              }
            }
            .getOrElse(true)
      }

      val constraints = parseConstraints(propertyElement)

      val defaultValueValidation = if (hasDefault) {
        val dvOpt = propertyElement match {
          case f: VariableElement   => getLiteralInitializerValue(f)
          case m: ExecutableElement => getLiteralReturnValue(m)
        }
        dvOpt match {
          case Some(dv) =>
            parseType(propertyTypeMirror, propertyElement, constraints)
              .andThen { pType =>
                validateDefaultValue(propertyElement, propertyName, pType, dv)
                  .map(_ => pType)
              }
          case None =>
            parseType(propertyTypeMirror, propertyElement, constraints)
        }
      } else {
        parseType(propertyTypeMirror, propertyElement, constraints)
      }

      defaultValueValidation.map { pType =>
        Property(
          propertyName,
          pType,
          isNullable,
          hasDefault,
          namingPatternSub,
          configName,
          ClassName.get(element),
          propertyElement
        )
      }
    }.sequence
  }

  private def getSettings(element: TypeElement): ConfigSettings = {
    val namingPattern = Option(
      typesUtil.getAnnotation(element, classOf[NamingPattern])
    )
    val source =
      Option(typesUtil.getAnnotation(element, classOf[Source])).map(_.value())
    val config = Option(typesUtil.getAnnotation(element, classOf[Config]))
      .getOrElse {
        MessagerUtils.error(
          element,
          s"Class ${element.getSimpleName} does not have a @Config annotation"
        )
        throw new IllegalStateException(
          s"Config ${element.getSimpleName} is missing @Config annotation"
        )
      }

    ConfigSettings(
      namingPattern,
      source,
      config.requireSerialization(),
      config.requireDynamicInitialization()
    )
  }

  private def parseType(
      tpe: TypeMirror,
      element: Element,
      constraints: List[Constraint]
  ): ValidatedNel[ParserError, PropertyType] = {
    verifyConstraintCompatibility(
      element,
      element.getSimpleName.toString,
      tpe,
      constraints
    ).andThen { _ =>
      val wrapped = TypeMirrorWrapper.wrap(tpe)
      val typeArguments =
        tpe match {
          case declaredType: DeclaredType =>
            declaredType.getTypeArguments.asScala.toList
          case _ => Nil
        }

      if (tpe.getKind.isPrimitive) {
        PropertyType.Primitive(TypeName.get(tpe), constraints).validNel
      } else if (wrapped.isEnum) {
        val scheme =
          Option(typesUtil.getAnnotation(element, classOf[EnumParsingScheme]))
            .map(_.value())
            .getOrElse(EnumParsingSchemes.EXACT_MATCH)
        PropertyType
          .EnumType(
            ClassName.get(
              tpe
                .asInstanceOf[DeclaredType]
                .asElement()
                .asInstanceOf[TypeElement]
            ),
            scheme,
            constraints
          )
          .validNel
      } else if (typesUtil.isConfigType(tpe)) {
        PropertyType
          .ConfigProperty(
            ClassName.get(
              tpe
                .asInstanceOf[DeclaredType]
                .asElement()
                .asInstanceOf[TypeElement]
            ),
            constraints
          )
          .validNel
      } else if (typesUtil.isCollection(tpe)) {
        if (typeArguments.isEmpty) {
          ParserError(
            element,
            s"Collection type ${tpe} is missing generic type arguments"
          ).invalidNel
        } else {
          val elemTpe = typeArguments.head
          val elemConstraints = parseConstraints(elemTpe)
          parseType(elemTpe, element, elemConstraints).map { elem =>
            if (typesUtil.isSet(tpe)) {
              PropertyType.Set(elem, constraints)
            } else {
              PropertyType.ListProperty(elem, constraints)
            }
          }
        }
      } else if (typesUtil.isMap(tpe)) {
        if (typeArguments.size < 2) {
          ParserError(
            element,
            s"Map type ${tpe} is missing generic type arguments"
          ).invalidNel
        } else {
          val keyTpe = typeArguments.head
          val valTpe = typeArguments(1)
          val keyConstraints = parseConstraints(keyTpe)
          val valConstraints = parseConstraints(valTpe)
          (
            parseType(keyTpe, element, keyConstraints),
            parseType(valTpe, element, valConstraints)
          ).mapN { (key, value) =>
            PropertyType.Map(key, value, constraints)
          }
        }
      } else {
        val canonicalName = wrapped.erasure().getQualifiedName
        if (canonicalName == classOf[Optional[?]].getName) {
          if (typeArguments.isEmpty) {
            ParserError(
              element,
              "Optional type is missing generic type argument"
            ).invalidNel
          } else {
            val elemTpe = typeArguments.head
            val elemConstraints = parseConstraints(elemTpe)
            parseType(elemTpe, element, elemConstraints).map { elem =>
              PropertyType.OptionalProperty(elem)
            }
          }
        } else {
          PropertyType
            .Object(
              ClassName.get(
                tpe
                  .asInstanceOf[DeclaredType]
                  .asElement()
                  .asInstanceOf[TypeElement]
              ),
              constraints
            )
            .validNel
        }
      }
    }
  }

  private def parseConstraints(
      construct: AnnotatedConstruct
  ): List[Constraint] = {
    construct.getAnnotationMirrors.asScala.flatMap { mirror =>
      val qName = mirror.getAnnotationType.asElement
        .asInstanceOf[TypeElement]
        .getQualifiedName
        .toString
      qName match {
        case "me.bristermitten.mittenlib.config.validation.Positive" |
            "jakarta.validation.constraints.Positive" |
            "javax.validation.constraints.Positive" =>
          Some(Constraint.Positive)
        case "me.bristermitten.mittenlib.config.validation.Negative" |
            "jakarta.validation.constraints.Negative" |
            "javax.validation.constraints.Negative" =>
          Some(Constraint.Negative)
        case "me.bristermitten.mittenlib.config.validation.Min" |
            "jakarta.validation.constraints.Min" |
            "javax.validation.constraints.Min" =>
          val value = getAnnotationNumericValue(mirror, "value").getOrElse(0.0)
          Some(Constraint.Min(value))
        case "me.bristermitten.mittenlib.config.validation.Max" |
            "jakarta.validation.constraints.Max" |
            "javax.validation.constraints.Max" =>
          val value = getAnnotationNumericValue(mirror, "value").getOrElse(0.0)
          Some(Constraint.Max(value))
        case "me.bristermitten.mittenlib.config.validation.Range" |
            "org.hibernate.validator.constraints.Range" =>
          val min = getAnnotationNumericValue(mirror, "min").getOrElse(0.0)
          val max = getAnnotationNumericValue(mirror, "max").getOrElse(0.0)
          Some(Constraint.Range(min, max))
        case "me.bristermitten.mittenlib.config.validation.NotBlank" |
            "jakarta.validation.constraints.NotBlank" |
            "javax.validation.constraints.NotBlank" =>
          Some(Constraint.NotBlank)
        case "me.bristermitten.mittenlib.config.validation.ValidateWith" =>
          getAnnotationValue[TypeMirror](mirror, "value").map { tpe =>
            Constraint.Custom(
              ClassName.get(
                tpe
                  .asInstanceOf[DeclaredType]
                  .asElement()
                  .asInstanceOf[TypeElement]
              )
            )
          }
        case _ => None
      }
    }.toList
  }

  private def getAnnotationValue[T](
      mirror: AnnotationMirror,
      key: String
  ): Option[T] = {
    mirror.getElementValues.asScala
      .find { case (element, _) =>
        element.getSimpleName.toString == key
      }
      .map { case (_, value) =>
        value.getValue.asInstanceOf[T]
      }
  }

  private def getAnnotationNumericValue(
      mirror: AnnotationMirror,
      key: String
  ): Option[Double] = {
    mirror.getElementValues.asScala
      .find { case (element, _) =>
        element.getSimpleName.toString == key
      }
      .flatMap { case (_, value) =>
        value.getValue match {
          case n: Number => Some(n.doubleValue())
          case _         => None
        }
      }
  }

  private def getASTLeaf(element: Element): Option[Tree] =
    trees.flatMap { t =>
      Option(t.getPath(element)).map(_.getLeaf)
    }

  private def getLiteralInitializerValue(
      element: VariableElement
  ): Option[Any] = {
    getASTLeaf(element).flatMap {
      case vt: VariableTree =>
        Option(vt.getInitializer).flatMap { init =>
          getExpressionValue(init)
        }
      case _ => None
    }
  }

  private def getLiteralReturnValue(element: ExecutableElement): Option[Any] = {
    getASTLeaf(element).flatMap {
      case mt: MethodTree =>
        Option(mt.getBody).flatMap { body =>
          body.getStatements.asScala.collectFirst { case rt: ReturnTree =>
            getExpressionValue(rt.getExpression)
          }.flatten
        }
      case _ => None
    }
  }

  private def getExpressionValue(tree: ExpressionTree): Option[Any] = {
    tree match {
      case lt: LiteralTree => Some(lt.getValue)
      case ut: UnaryTree if ut.getKind == Tree.Kind.UNARY_MINUS =>
        getExpressionValue(ut.getExpression).map {
          case n: Number => -n.doubleValue()
          case other     => other
        }
      case _ => None
    }
  }

  private def validateDefaultValue(
      element: Element,
      name: String,
      propertyType: PropertyType,
      defaultValue: Any
  ): ValidatedNel[ParserError, Unit] = {
    val violations = getConstraints(propertyType).flatMap { constraint =>
      validateValueAgainstConstraint(defaultValue, constraint).map(msg =>
        s"Default value $defaultValue for property '$name' is invalid: $msg"
      )
    }

    if (violations.isEmpty) ().validNel
    else violations.map(msg => ParserError(element, msg)).toNel.get.invalid
  }

  private def getConstraints(pt: PropertyType): List[Constraint] = pt match {
    case PropertyType.Primitive(_, cs)       => cs
    case PropertyType.Object(_, cs)          => cs
    case PropertyType.ListProperty(elem, cs) => cs ++ getConstraints(elem)
    case PropertyType.Map(key, value, cs)    =>
      cs ++ getConstraints(key) ++ getConstraints(value)
    case PropertyType.Set(elem, cs)          => cs ++ getConstraints(elem)
    case PropertyType.ConfigProperty(_, cs)  => cs
    case PropertyType.EnumType(_, _, cs)     => cs
    case PropertyType.OptionalProperty(elem) => getConstraints(elem)
  }

  private def validateValueAgainstConstraint(
      value: Any,
      constraint: Constraint
  ): Option[String] = {
    constraint match {
      case Constraint.Positive =>
        value match {
          case n: Number if n.doubleValue() <= 0 => Some("Must be positive")
          case _                                 => None
        }
      case Constraint.Negative =>
        value match {
          case n: Number if n.doubleValue() >= 0 => Some("Must be negative")
          case _                                 => None
        }
      case Constraint.Min(min) =>
        value match {
          case n: Number if n.doubleValue() < min =>
            Some(s"Must be at least $min")
          case _ => None
        }
      case Constraint.Max(max) =>
        value match {
          case n: Number if n.doubleValue() > max =>
            Some(s"Must be at most $max")
          case _ => None
        }
      case Constraint.Range(min, max) =>
        value match {
          case n: Number if n.doubleValue() < min || n.doubleValue() > max =>
            Some(s"Must be between $min and $max")
          case _ => None
        }
      case Constraint.NotBlank =>
        value match {
          case s: String if s.trim.isEmpty => Some("Must not be blank")
          case _                           => None
        }
      case _ => None
    }
  }

  private def verifyConstraintCompatibility(
      element: Element,
      name: String,
      tpe: TypeMirror,
      constraints: List[Constraint]
  ): ValidatedNel[ParserError, Unit] = {
    val isNumeric = isNumericType(tpe)
    val isString = isStringType(tpe)
    val typeNameStr = TypeName.get(tpe).toString

    val errors = constraints.flatMap {
      case Constraint.Positive =>
        if (!isNumeric)
          Some(
            ParserError(
              element,
              s"Constraint annotation @Positive cannot be applied to type $typeNameStr. Expected a numeric type."
            )
          )
        else None
      case Constraint.Negative =>
        if (!isNumeric)
          Some(
            ParserError(
              element,
              s"Constraint annotation @Negative cannot be applied to type $typeNameStr. Expected a numeric type."
            )
          )
        else None
      case Constraint.Min(_) =>
        if (!isNumeric)
          Some(
            ParserError(
              element,
              s"Constraint annotation @Min cannot be applied to type $typeNameStr. Expected a numeric type."
            )
          )
        else None
      case Constraint.Max(_) =>
        if (!isNumeric)
          Some(
            ParserError(
              element,
              s"Constraint annotation @Max cannot be applied to type $typeNameStr. Expected a numeric type."
            )
          )
        else None
      case Constraint.Range(_, _) =>
        if (!isNumeric)
          Some(
            ParserError(
              element,
              s"Constraint annotation @Range cannot be applied to type $typeNameStr. Expected a numeric type."
            )
          )
        else None
      case Constraint.NotBlank =>
        if (!isString)
          Some(
            ParserError(
              element,
              s"Constraint annotation @NotBlank cannot be applied to type $typeNameStr. Expected a String or CharSequence."
            )
          )
        else None
      case Constraint.Custom(validatorClass) =>
        verifyCustomValidator(element, validatorClass, tpe)
    }

    if (errors.isEmpty) ().validNel
    else errors.toNel.get.invalid
  }

  private def verifyCustomValidator(
      element: Element,
      validatorClass: ClassName,
      propertyType: TypeMirror
  ): Option[ParserError] = {
    val validatorElement =
      elements.getTypeElement(classOf[Validator[?]].getName)
    val customElement = elements.getTypeElement(validatorClass.canonicalName())
    if (customElement == null) {
      Some(
        ParserError(
          element,
          s"Custom validator class ${validatorClass.canonicalName()} not found"
        )
      )
    } else {
      val boxedType = if (propertyType.getKind.isPrimitive) {
        types.boxedClass(propertyType.asInstanceOf[PrimitiveType]).asType()
      } else {
        propertyType
      }
      val wildcard = types.getWildcardType(null, boxedType)
      val expectedValidatorType =
        types.getDeclaredType(validatorElement, wildcard)
      val propertyTypeNameStr = TypeName.get(propertyType).toString
      if (!types.isAssignable(customElement.asType(), expectedValidatorType)) {
        Some(
          ParserError(
            element,
            s"Constraint annotation @ValidateWith(${validatorClass.simpleName()}.class) cannot be applied to type $propertyTypeNameStr. Expected a Validator compatible with $propertyTypeNameStr."
          )
        )
      } else {
        None
      }
    }
  }

  private def isNumericType(typeMirror: TypeMirror): Boolean = {
    if (typeMirror.getKind.isPrimitive) {
      typeMirror.getKind match {
        case TypeKind.BYTE | TypeKind.SHORT | TypeKind.INT | TypeKind.LONG |
            TypeKind.FLOAT | TypeKind.DOUBLE =>
          true
        case _ => false
      }
    } else {
      val element = types.asElement(typeMirror)
      if (element != null && element.isInstanceOf[TypeElement]) {
        val typeStr =
          element.asInstanceOf[TypeElement].getQualifiedName.toString
        typeStr == classOf[java.lang.Byte].getName ||
        typeStr == classOf[java.lang.Short].getName ||
        typeStr == classOf[java.lang.Integer].getName ||
        typeStr == classOf[java.lang.Long].getName ||
        typeStr == classOf[java.lang.Float].getName ||
        typeStr == classOf[java.lang.Double].getName ||
        typeStr == classOf[java.math.BigInteger].getName ||
        typeStr == classOf[java.math.BigDecimal].getName
      } else {
        false
      }
    }
  }

  private def isStringType(typeMirror: TypeMirror): Boolean = {
    val element = types.asElement(typeMirror)
    if (element != null && element.isInstanceOf[TypeElement]) {
      val typeStr = element.asInstanceOf[TypeElement].getQualifiedName.toString
      typeStr == classOf[java.lang.String].getName || typeStr == classOf[
        java.lang.CharSequence
      ].getName
    } else {
      false
    }
  }

  private def configStructureParents(
      structure: ConfigStructure
  ): List[ClassName] =
    structure match {
      case a: ConfigStructure.Atomic       => a.parentClass.toList
      case i: ConfigStructure.Intersection => i.roots
      case u: ConfigStructure.Union        => u.parents
    }

  private def buildEnclosedIn(name: ClassName): ASTParentReference = {
    val enclosingName = name.enclosingClassName()
    if (enclosingName == null) return null
    val parentRef = buildEnclosedIn(enclosingName)
    val enclosingDomain = configNameCache.lookupDomain(enclosingName)
    val isInterface = enclosingDomain.exists {
      case a: ConfigStructure.Atomic => a.isInterface
      case _                         => false
    }
    val manualClassName = enclosingDomain.flatMap { ast =>
      val enclosingElement = elements.getTypeElement(ast.name.canonicalName())
      Option(typesUtil.getAnnotation(enclosingElement, classOf[Config]))
        .filter(_.className().nonEmpty)
        .map(_.className())
    }.orNull
    ASTParentReference(enclosingName, isInterface, manualClassName, parentRef)
  }

  private def createAbstractStructure(
      ast: ConfigStructure,
      element: TypeElement
  ): AbstractConfigStructure = {
    val name = ast.name
    val configAnnotation = typesUtil.getAnnotation(element, classOf[Config])
    val sourceAnnotation = typesUtil.getAnnotation(element, classOf[Source])
    val namingPatternAnnotation =
      typesUtil.getAnnotation(element, classOf[NamingPattern])
    val astSettings = ASTSettings.ConfigASTSettings(
      namingPatternAnnotation,
      sourceAnnotation,
      configAnnotation,
      false
    )
    val enclosedIn = buildEnclosedIn(name)

    val configTypeSource: ConfigTypeSource = if (element.getKind.isInterface) {
      val parents = new java.util.ArrayList[TypeMirror]()
      element.getInterfaces.forEach(parents.add)
      ConfigTypeSource.InterfaceConfigTypeSource(element, parents)
    } else {
      val superclass = element.getSuperclass
      val parentOpt =
        if (
          superclass.getKind != TypeKind.NONE && superclass.toString != "java.lang.Object"
        ) {
          java.util.Optional.of(superclass)
        } else {
          java.util.Optional.empty[TypeMirror]()
        }
      ConfigTypeSource.ClassConfigTypeSource(element, parentOpt)
    }

    // Get already-cached enclosed AbstractConfigStructures
    val enclosedAst = new java.util.ArrayList[AbstractConfigStructure]()
    for (enclosed <- ast.enclosed) {
      configNameCache.lookupAST(enclosed.name).ifPresent(enclosedAst.add)
    }

    // Convert domain properties to AST properties
    val astProperties = new java.util.ArrayList[ASTProperty]()
    for (prop <- ast.properties) {
      val propSource: ASTProperty.PropertySource =
        if (prop.element.getKind.isField) {
          ASTProperty.PropertySource.FieldSource(
            prop.element.asInstanceOf[javax.lang.model.element.VariableElement]
          )
        } else {
          ASTProperty.PropertySource.MethodSource(
            prop.element
              .asInstanceOf[javax.lang.model.element.ExecutableElement]
          )
        }
      val fieldNamingPattern =
        typesUtil.getAnnotation(prop.element, classOf[NamingPattern])
      val effectiveNamingPattern =
        if (fieldNamingPattern != null) fieldNamingPattern
        else namingPatternAnnotation
      val astPropSettings = ASTSettings.PropertyASTSettings(
        effectiveNamingPattern,
        typesUtil.getAnnotation(prop.element, classOf[ConfigName]),
        typesUtil
          .getAnnotation(prop.element, classOf[EnumParsingScheme]) match {
          case null =>
            me.bristermitten.mittenlib.config.EnumParsingSchemes.EXACT_MATCH
          case a => a.value()
        },
        prop.isNullable,
        prop.hasDefault,
        new java.util.ArrayList[ValidationConstraint](),
        new java.util.ArrayList[ValidationConstraint](),
        new java.util.ArrayList[ValidationConstraint]()
      )
      astProperties.add(
        ASTProperty(prop.name, prop.typeMirror, propSource, astPropSettings)
      )
    }

    ast match {
      case _: ConfigStructure.Union =>
        AbstractConfigStructure.Union(
          name,
          configTypeSource,
          astSettings,
          enclosedIn,
          new java.util.ArrayList[ClassName](),
          enclosedAst,
          astProperties
        )
      case i: ConfigStructure.Intersection =>
        AbstractConfigStructure.Intersection(
          name,
          configTypeSource,
          astSettings,
          enclosedIn,
          enclosedAst,
          new java.util.ArrayList[ClassName](i.roots.asJava),
          astProperties
        )
      case _ =>
        AbstractConfigStructure.Atomic(
          name,
          configTypeSource,
          astSettings,
          enclosedAst,
          enclosedIn,
          astProperties
        )
    }
  }

  private def putInCache(ast: ConfigStructure, element: TypeElement): Unit = {
    // Put domain entry first so enclosed configs can find this parent in buildEnclosedIn
    configNameCache.putDomain(ast)
    // Cache enclosed configs (they need parent in domain cache for buildEnclosedIn)
    for (enclosed <- ast.enclosed) {
      val enclosedElement =
        elements.getTypeElement(enclosed.name.canonicalName())
      if (enclosedElement != null) {
        putInCache(enclosed, enclosedElement)
      }
    }
    // Create abstract AST after enclosed abstract ASTs are in cache
    val abstractAst = createAbstractStructure(ast, element)
    configNameCache.put(abstractAst)
    generatedTypeCache.put(
      element,
      classNameGenerator.getConcreteConfigClassName(ast).canonicalName()
    )
  }
