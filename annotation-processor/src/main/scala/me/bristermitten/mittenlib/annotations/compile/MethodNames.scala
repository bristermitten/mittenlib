package me.bristermitten.mittenlib.annotations.compile

import com.google.inject.{Inject, Singleton}
import java.util.{HashMap, HashSet, Map, Set}
import javax.lang.model.element.{ExecutableElement, TypeElement, VariableElement}
import me.bristermitten.mittenlib.annotations.ast.Property
import me.bristermitten.mittenlib.annotations.util.ElementsFinder
import me.bristermitten.mittenlib.util.Strings
import scala.jdk.CollectionConverters.*

@Singleton
class MethodNames @Inject() (
  private val elementsFinder: ElementsFinder
):
  private val safeNameCache = new HashMap[VariableElement, String]()
  private val methodNamesCache = new HashMap[VariableElement, Set[String]]()

  private val SERIALIZE_METHOD_PREFIX = "serialize"
  private val DESERIALIZE_METHOD_PREFIX = "deserialize"

  def safeMethodName(variableElement: VariableElement, enclosingClass: TypeElement): String =
    safeNameCache.computeIfAbsent(variableElement, elem => safeMethodName0(elem, enclosingClass))

  def safeMethodName(property: Property): String =
    property.source() match {
      case fieldSource: Property.PropertySource.FieldSource =>
        val field = fieldSource.element()
        safeMethodName(field, field.getEnclosingElement().asInstanceOf[TypeElement])
      case methodSource: Property.PropertySource.MethodSource =>
        methodSource.element().getSimpleName().toString()
    }

  private def safeMethodName0(variableElement: VariableElement, enclosingClass: TypeElement): String =
    val methodNames = methodNamesCache.computeIfAbsent(variableElement, _ => getNoArgMethodNames(enclosingClass))
    val name = new StringBuilder(variableElement.getSimpleName().toString())
    while (methodNames.contains(name.toString())) {
      name.append("_")
    }
    name.toString()

  private def getNoArgMethodNames(enclosingClass: TypeElement): Set[String] =
    val names = new HashSet[String]()
    for (method <- elementsFinder.getAllMethods(enclosingClass).asScala) {
      if (method.getParameters().isEmpty()) {
        names.add(method.getSimpleName().toString())
      }
    }
    names

  def getDeserializeMethodName(property: Property): String =
    val name = Strings.capitalize(property.name())
    DESERIALIZE_METHOD_PREFIX + name

  def getSerializeMethodName(property: Property): String =
    val name = Strings.capitalize(property.name())
    SERIALIZE_METHOD_PREFIX + name
