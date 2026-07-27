package me.bristermitten.mittenlib.annotations.compile

import com.google.inject.{Inject, Singleton}

import javax.lang.model.element.{
  ExecutableElement,
  TypeElement,
  VariableElement
}
import me.bristermitten.mittenlib.annotations.domain.Property
import me.bristermitten.mittenlib.annotations.util.ElementsFinder
import me.bristermitten.mittenlib.util.Strings

import scala.collection.mutable
import scala.jdk.CollectionConverters.*

@Singleton
class MethodNames @Inject() (private val elementsFinder: ElementsFinder):
  private val safeNameCache = new mutable.HashMap[VariableElement, String]()
  private val methodNamesCache =
    new mutable.HashMap[VariableElement, mutable.Set[String]]()

  private val SERIALIZE_METHOD_PREFIX = "serialize"
  private val DESERIALIZE_METHOD_PREFIX = "deserialize"

  def safeMethodName(
      variableElement: VariableElement,
      enclosingClass: TypeElement
  ): String =
    safeNameCache.getOrElseUpdate(
      variableElement,
      { safeMethodName0(variableElement, enclosingClass) }
    )

  def safeMethodName(property: Property): String =
    if (property.element.getKind.isField) {
      val field = property.element.asInstanceOf[VariableElement]
      safeMethodName(field, field.getEnclosingElement.asInstanceOf[TypeElement])
    } else {
      property.element.getSimpleName.toString
    }

  private def safeMethodName0(
      variableElement: VariableElement,
      enclosingClass: TypeElement
  ): String =
    val methodNames = methodNamesCache.getOrElseUpdate(
      variableElement,
      { getNoArgMethodNames(enclosingClass) }
    )
    val name = new StringBuilder(variableElement.getSimpleName.toString)
    while (methodNames.contains(name.toString())) {
      name.append("_")
    }
    name.toString()

  private def getNoArgMethodNames(
      enclosingClass: TypeElement
  ): mutable.Set[String] =
    val names = new mutable.HashSet[String]()
    for (method <- elementsFinder.getAllMethods(enclosingClass)) {
      if (method.getParameters.isEmpty) {
        names.add(method.getSimpleName.toString)
      }
    }
    names

  def getDeserializeMethodName(property: Property): String =
    val name = Strings.capitalize(property.name)
    DESERIALIZE_METHOD_PREFIX + name

  def getSerializeMethodName(property: Property): String =
    val name = Strings.capitalize(property.name)
    SERIALIZE_METHOD_PREFIX + name
