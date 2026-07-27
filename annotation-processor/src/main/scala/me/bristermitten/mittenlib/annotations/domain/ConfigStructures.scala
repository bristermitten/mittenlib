package me.bristermitten.mittenlib.annotations.domain

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
    requireDynamicInitialization: Boolean,
    className: Option[String] = None
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
