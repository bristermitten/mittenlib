package me.bristermitten.mittenlib.annotations.util

import javax.lang.model.element.Element
import javax.lang.model.element.Name
import javax.lang.model.element.VariableElement
import javax.lang.model.`type`.TypeMirror
import org.mockito.Mockito._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class StringifyTest extends AnyFunSuite with Matchers {

  test("testPrettyStringifyGenericElement") {
    val element = mock(classOf[Element])
    when(element.toString).thenReturn("SomeGenericElement")

    val result = Stringify.prettyStringify(element)
    result shouldBe "SomeGenericElement"
  }

  test("testPrettyStringifyVariableElement") {
    val variableElement = mock(classOf[VariableElement])
    val typeMirror = mock(classOf[TypeMirror])
    val name = mock(classOf[Name])
    val enclosingElement = mock(classOf[Element])

    when(typeMirror.toString).thenReturn("int")
    when(name.toString).thenReturn("myField")
    when(enclosingElement.toString).thenReturn("MyClass")

    when(variableElement.asType()).thenReturn(typeMirror)
    when(variableElement.getSimpleName).thenReturn(name)
    when(variableElement.getEnclosingElement).thenReturn(enclosingElement)

    val result = Stringify.prettyStringify(variableElement)
    result shouldBe "int myField in class MyClass"
  }
}
