package me.bristermitten.mittenlib.annotations.util

import com.palantir.javapoet.ClassName
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier
import javax.lang.model.element.NestingKind
import javax.lang.model.element.TypeElement
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.RecordComponentElement
import javax.lang.model.`type`.TypeMirror
import javax.lang.model.util.ElementFilter
import me.bristermitten.mittenlib.config.Newtype
import scala.jdk.CollectionConverters.*

object NewtypeUtil:

  def isNewtype(element: Element): Boolean =
    element.isInstanceOf[TypeElement] && element.getAnnotation(
      classOf[Newtype]
    ) != null

  def getAbstractMethods(element: TypeElement): List[ExecutableElement] =
    ElementFilter
      .methodsIn(element.getEnclosedElements)
      .asScala
      .filter(m => !m.isDefault && !m.getModifiers.contains(Modifier.STATIC))
      .toList

  def getNewtypeRecordComponent(element: TypeElement): RecordComponentElement =
    val components = element.getRecordComponents.asScala.toList
    if (components.size != 1) {
      throw new IllegalArgumentException(
        s"Newtype record $element must have exactly one component"
      )
    }
    components.head

  def getAbstractMethod(element: TypeElement): ExecutableElement =
    val methods = getAbstractMethods(element)
    if (methods.size != 1) {
      throw new IllegalArgumentException(
        s"Newtype interface $element must have exactly one abstract method"
      )
    }
    methods.head

  def getUnderlyingType(element: TypeElement): TypeMirror =
    if (element.getKind == ElementKind.RECORD) {
      getNewtypeRecordComponent(element).asType()
    } else if (element.getKind == ElementKind.INTERFACE) {
      getAbstractMethod(element).getReturnType
    } else {
      throw new IllegalArgumentException(
        s"Newtype annotation only supports records and interfaces: $element"
      )
    }

  def getAccessorName(element: TypeElement): String =
    if (element.getKind == ElementKind.RECORD) {
      s"${getNewtypeRecordComponent(element).getSimpleName}()"
    } else if (element.getKind == ElementKind.INTERFACE) {
      s"${getAbstractMethod(element).getSimpleName}()"
    } else {
      throw new IllegalArgumentException(
        s"Newtype annotation only supports records and interfaces: $element"
      )
    }

  def getImplClassName(element: TypeElement): ClassName =
    val publicClass = ClassName.get(element)
    if (element.getKind == ElementKind.RECORD) {
      publicClass
    } else if (element.getNestingKind == NestingKind.MEMBER) {
      val nameBuilder =
        new java.lang.StringBuilder(element.getSimpleName.toString)
      var enclosing = element.getEnclosingElement
      while (enclosing.isInstanceOf[TypeElement]) {
        nameBuilder.insert(0, enclosing.getSimpleName.toString)
        enclosing = enclosing.getEnclosingElement
      }
      nameBuilder.append("Impl")
      ClassName.get(publicClass.packageName(), nameBuilder.toString)
    } else {
      publicClass.peerClass(s"${publicClass.simpleName()}Impl")
    }
