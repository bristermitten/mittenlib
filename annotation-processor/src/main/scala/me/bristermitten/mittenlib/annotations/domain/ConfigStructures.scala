package me.bristermitten.mittenlib.annotations

package ast {
  import com.palantir.javapoet.ClassName
  import com.palantir.javapoet.TypeName
  import javax.lang.model.element.{
    Element,
    TypeElement,
    VariableElement,
    ExecutableElement
  }
  import javax.lang.model.`type`.TypeMirror
  import me.bristermitten.mittenlib.config.names.NamingPattern
  import me.bristermitten.mittenlib.config.names.ConfigName
  import me.bristermitten.mittenlib.config.Source
  import me.bristermitten.mittenlib.config.Config
  import me.bristermitten.mittenlib.config.EnumParsingSchemes

  trait ASTNode {
    def settings(): ASTSettings
  }

  case class ASTParentReference(
      parentClassName: ClassName,
      isInterface: Boolean,
      manualClassName: String,
      parent: ASTParentReference
  )

  sealed trait ASTSettings {
    def namingPattern(): NamingPattern
  }
  object ASTSettings {
    case class ConfigASTSettings(
        namingPatternField: NamingPattern,
        sourceField: Source,
        configField: Config,
        generateToStringField: Boolean
    ) extends ASTSettings {
      def namingPattern(): NamingPattern = namingPatternField
      def source(): Source = sourceField
      def config(): Config = configField
      def generateToString(): Boolean = generateToStringField
    }

    case class PropertyASTSettings(
        namingPatternField: NamingPattern,
        configNameField: ConfigName,
        enumParsingSchemeField: EnumParsingSchemes,
        isNullableField: Boolean,
        hasDefaultValueField: Boolean,
        constraintsField: java.util.List[ValidationConstraint],
        elementConstraintsField: java.util.List[ValidationConstraint],
        keyConstraintsField: java.util.List[ValidationConstraint]
    ) extends ASTSettings {
      def namingPattern(): NamingPattern = namingPatternField
      def configName(): ConfigName = configNameField
      def enumParsingScheme(): EnumParsingSchemes = enumParsingSchemeField
      def isNullable(): Boolean = isNullableField
      def hasDefaultValue(): Boolean = hasDefaultValueField
      def constraints(): java.util.List[ValidationConstraint] = constraintsField
      def elementConstraints(): java.util.List[ValidationConstraint] =
        elementConstraintsField
      def keyConstraints(): java.util.List[ValidationConstraint] =
        keyConstraintsField
    }
  }

  sealed trait ConfigTypeSource {
    def element(): TypeElement
    def parents(): java.util.List[TypeMirror]
  }
  object ConfigTypeSource {
    case class ClassConfigTypeSource(
        elementField: TypeElement,
        parentField: java.util.Optional[TypeMirror]
    ) extends ConfigTypeSource {
      def element(): TypeElement = elementField
      def parents(): java.util.List[TypeMirror] = {
        val list = new java.util.ArrayList[TypeMirror]()
        parentField.ifPresent(list.add)
        list
      }
    }

    case class InterfaceConfigTypeSource(
        elementField: TypeElement,
        parentsField: java.util.List[TypeMirror]
    ) extends ConfigTypeSource {
      def element(): TypeElement = elementField
      def parents(): java.util.List[TypeMirror] = parentsField
    }
  }

  case class Property(
      nameField: String,
      propertyTypeField: TypeMirror,
      sourceField: Property.PropertySource,
      settingsField: ASTSettings.PropertyASTSettings
  ) extends ASTNode {
    def name(): String = nameField
    def propertyType(): TypeMirror = propertyTypeField
    def source(): Property.PropertySource = sourceField
    def settings(): ASTSettings.PropertyASTSettings = settingsField
  }
  object Property {
    sealed trait PropertySource {
      def element(): Element
    }
    object PropertySource {
      case class FieldSource(elementField: VariableElement)
          extends PropertySource {
        def element(): Element = elementField
      }
      case class MethodSource(elementField: ExecutableElement)
          extends PropertySource {
        def element(): Element = elementField
      }
    }
  }

  sealed trait ValidationConstraint
  object ValidationConstraint {
    case class Positive() extends ValidationConstraint
    case class Negative() extends ValidationConstraint
    case class Min(value: Double) extends ValidationConstraint
    case class Max(value: Double) extends ValidationConstraint
    case class NotBlank() extends ValidationConstraint
    case class Range(min: Double, max: Double) extends ValidationConstraint
    case class Custom(validatorClassName: ClassName)
        extends ValidationConstraint
  }

  sealed trait AbstractConfigStructure {
    def name(): ClassName
    def enclosedIn(): ASTParentReference
    def enclosed(): java.util.List[AbstractConfigStructure]
    def properties(): java.util.List[Property]
    def source(): ConfigTypeSource
    def settings(): ASTSettings.ConfigASTSettings
  }
  object AbstractConfigStructure {
    case class Atomic(
        nameField: ClassName,
        sourceField: ConfigTypeSource,
        settingsField: ASTSettings.ConfigASTSettings,
        enclosedList: java.util.List[AbstractConfigStructure],
        enclosedInField: ASTParentReference,
        propertiesList: java.util.List[Property]
    ) extends AbstractConfigStructure {
      def name(): ClassName = nameField
      def enclosedIn(): ASTParentReference = enclosedInField
      def enclosed(): java.util.List[AbstractConfigStructure] = enclosedList
      def properties(): java.util.List[Property] = propertiesList
      def source(): ConfigTypeSource = sourceField
      def settings(): ASTSettings.ConfigASTSettings = settingsField
    }

    case class Intersection(
        nameField: ClassName,
        sourceField: ConfigTypeSource,
        settingsField: ASTSettings.ConfigASTSettings,
        enclosedInField: ASTParentReference,
        enclosedList: java.util.List[AbstractConfigStructure],
        roots: java.util.List[ClassName],
        propertiesList: java.util.List[Property]
    ) extends AbstractConfigStructure {
      def name(): ClassName = nameField
      def enclosedIn(): ASTParentReference = enclosedInField
      def enclosed(): java.util.List[AbstractConfigStructure] = enclosedList
      def properties(): java.util.List[Property] = propertiesList
      def source(): ConfigTypeSource = sourceField
      def settings(): ASTSettings.ConfigASTSettings = settingsField
    }

    case class Union(
        nameField: ClassName,
        sourceField: ConfigTypeSource,
        settingsField: ASTSettings.ConfigASTSettings,
        enclosedInField: ASTParentReference,
        parents: java.util.List[ClassName],
        alternatives: java.util.List[AbstractConfigStructure],
        propertiesList: java.util.List[Property]
    ) extends AbstractConfigStructure {
      def name(): ClassName = nameField
      def enclosedIn(): ASTParentReference = enclosedInField
      def enclosed(): java.util.List[AbstractConfigStructure] = alternatives
      def properties(): java.util.List[Property] = propertiesList
      def source(): ConfigTypeSource = sourceField
      def settings(): ASTSettings.ConfigASTSettings = settingsField
    }
  }
}

package domain {
  import com.palantir.javapoet.ClassName
  import com.palantir.javapoet.TypeName
  import javax.lang.model.element.{Element, ExecutableElement, TypeElement}
  import javax.lang.model.`type`.TypeMirror
  import me.bristermitten.mittenlib.config.names.NamingPattern
  import me.bristermitten.mittenlib.config.EnumParsingSchemes

  sealed trait Constraint
  object Constraint {
    case object Positive extends Constraint
    case object Negative extends Constraint
    case class Min(value: Double) extends Constraint
    case class Max(value: Double) extends Constraint
    case class Range(min: Double, max: Double) extends Constraint
    case object NotBlank extends Constraint
    case class Custom(validatorClassName: ClassName) extends Constraint
  }

  sealed trait PropertyType
  object PropertyType {
    case class Primitive(tpe: TypeName, constraints: List[Constraint])
        extends PropertyType
    case class EnumType(
        tpe: ClassName,
        scheme: EnumParsingSchemes,
        constraints: List[Constraint]
    ) extends PropertyType
    case class ConfigProperty(tpe: ClassName, constraints: List[Constraint])
        extends PropertyType
    case class ListProperty(elem: PropertyType, constraints: List[Constraint])
        extends PropertyType
    case class Set(elem: PropertyType, constraints: List[Constraint])
        extends PropertyType
    case class Map(
        key: PropertyType,
        value: PropertyType,
        constraints: List[Constraint]
    ) extends PropertyType
    case class OptionalProperty(elem: PropertyType) extends PropertyType
    case class Object(tpe: TypeName, constraints: List[Constraint])
        extends PropertyType
  }

  case class Property(
      name: String,
      propertyType: PropertyType,
      isNullable: Boolean,
      hasDefault: Boolean,
      namingPattern: Option[NamingPattern],
      configName: Option[String],
      dtoType: ClassName,
      element: Element
  ) {
    def typeMirror: TypeMirror = element match {
      case e: ExecutableElement => e.getReturnType
      case other                => other.asType()
    }
  }

  case class ConfigSettings(
      namingPattern: Option[NamingPattern],
      source: Option[String],
      requireSerialization: Boolean,
      requireDynamicInitialization: Boolean
  )

  sealed trait ConfigStructure {
    def name: ClassName
    def settings: ConfigSettings
    def properties: List[Property]
    def enclosed: List[ConfigStructure]
  }
  object ConfigStructure {
    case class Atomic(
        name: ClassName,
        isInterface: Boolean,
        parentClass: Option[ClassName],
        settings: ConfigSettings,
        properties: List[Property],
        enclosed: List[ConfigStructure]
    ) extends ConfigStructure

    case class Intersection(
        name: ClassName,
        roots: List[ClassName],
        settings: ConfigSettings,
        properties: List[Property],
        enclosed: List[ConfigStructure]
    ) extends ConfigStructure

    case class Union(
        name: ClassName,
        parents: List[ClassName],
        settings: ConfigSettings,
        enclosed: List[ConfigStructure],
        properties: List[Property]
    ) extends ConfigStructure:
      def alternatives: List[ConfigStructure] = enclosed
  }
}
