package me.bristermitten.mittenlib.annotations.util

import com.google.inject.{Inject, Singleton}
import com.palantir.javapoet.ClassName
import me.bristermitten.mittenlib.annotations.ast.AbstractConfigStructure
import me.bristermitten.mittenlib.annotations.compile.ConfigNameCache
import me.bristermitten.mittenlib.annotations.domain.*

@Singleton
class ConfigStructureAnalysis @Inject() (cache: ConfigNameCache):

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
