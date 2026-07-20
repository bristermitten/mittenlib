package me.bristermitten.mittenlib.annotations.util

import com.google.inject.{Inject, Singleton}
import com.palantir.javapoet.ClassName
import me.bristermitten.mittenlib.annotations.ast.{
  AbstractConfigStructure,
  Property => AstProperty
}
import me.bristermitten.mittenlib.annotations.compile.ConfigNameCache
import me.bristermitten.mittenlib.annotations.domain.*

import javax.lang.model.`type`.DeclaredType
import javax.lang.model.`type`.TypeMirror
import javax.lang.model.element.TypeElement

@Singleton
class ConfigStructureAnalysis @Inject() (
    private val cache: ConfigNameCache,
    private val typesUtil: TypesUtil
):

  def isDynamicallyInitializable(structure: ConfigStructure): Boolean =
    isDynamicallyInitializable(structure, Set.empty)

  def isDynamicallyInitializable(structure: AbstractConfigStructure): Boolean =
    cache.lookupDomain(structure.name()).exists(isDynamicallyInitializable)

  private def isDynamicallyInitializable(
      structure: ConfigStructure,
      visited: Set[ClassName]
  ): Boolean =
    val name = structure.name
    if (visited.contains(name)) true
    else
      val nextVisited = visited + name
      structure.properties.forall { p =>
        p.hasDefault || p.isNullable || isTypeInitializable(
          p.propertyType,
          nextVisited
        )
      }

  def isTypeInitializable(tpe: PropertyType): Boolean =
    isTypeInitializable(tpe, Set.empty)

  def isTypeInitializable(tpe: TypeMirror): Boolean =
    if (typesUtil.isConfigType(tpe)) {
      tpe match {
        case declaredType: DeclaredType =>
          val className = ClassName.get(
            declaredType.asElement().asInstanceOf[TypeElement]
          )
          val opt = cache.lookupDomain(className)
          opt.exists(isDynamicallyInitializable(_, Set.empty))
        case _ => false
      }
    } else {
      typesUtil.isOptional(tpe)
    }

  def hasDefaultOrIsInitializable(p: Property): Boolean =
    p.hasDefault || isTypeInitializable(p.propertyType)

  def hasDefaultOrIsInitializable(p: AstProperty): Boolean =
    p.settings().hasDefaultValue() || isTypeInitializable(p.propertyType())

  private def isTypeInitializable(
      tpe: PropertyType,
      visited: Set[ClassName]
  ): Boolean = tpe match {
    case PropertyType.ConfigProperty(className, _) =>
      val opt = cache.lookupDomain(className)
      if (opt.isDefined) isDynamicallyInitializable(opt.get, visited)
      else false
    case PropertyType.OptionalProperty(_) => true
    case _                                => false
  }

  def needsValidation(structure: ConfigStructure): Boolean =
    structure.properties.exists { p =>
      hasConstraints(p.propertyType) || (!isPrimitive(
        p.propertyType
      ) && !p.isNullable)
    }

  def needsValidation(structure: AbstractConfigStructure): Boolean =
    cache.lookupDomain(structure.name()).exists(needsValidation)

  private def hasConstraints(pt: PropertyType): Boolean = pt match {
    case PropertyType.Primitive(_, cs)       => cs.nonEmpty
    case PropertyType.Object(_, cs)          => cs.nonEmpty
    case PropertyType.ListProperty(elem, cs) =>
      cs.nonEmpty || hasConstraints(elem)
    case PropertyType.Map(key, value, cs) =>
      cs.nonEmpty || hasConstraints(key) || hasConstraints(value)
    case PropertyType.Set(elem, cs) => cs.nonEmpty || hasConstraints(elem)
    case PropertyType.ConfigProperty(_, cs)  => cs.nonEmpty
    case PropertyType.EnumType(_, _, cs)     => cs.nonEmpty
    case PropertyType.OptionalProperty(elem) => hasConstraints(elem)
  }

  private def isPrimitive(pt: PropertyType): Boolean = pt match {
    case PropertyType.Primitive(_, _) => true
    case _                            => false
  }
