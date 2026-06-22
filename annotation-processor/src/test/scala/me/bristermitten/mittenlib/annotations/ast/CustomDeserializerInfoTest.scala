package me.bristermitten.mittenlib.annotations.ast

import org.mockito.Mockito.mock
import javax.lang.model.element.TypeElement

class CustomDeserializerInfoTest extends munit.FunSuite:

  test("record methods") {
    val element1 = mock(classOf[TypeElement])
    val element2 = mock(classOf[TypeElement])

    val info1 = CustomDeserializerInfo(element1, true, false, true)
    val info2 = CustomDeserializerInfo(element1, true, false, true)
    val info3 = CustomDeserializerInfo(element2, false, true, false)

    assertEquals(info1.deserializerClass, element1)
    assertEquals(info1.isStatic, true)
    assertEquals(info1.isFallback, false)
    assertEquals(info1.isGlobal, true)

    assertEquals(info1, info2)
    assertNotEquals(info1, info3)

    assertEquals(info1.hashCode(), info2.hashCode())
    assertNotEquals(info1.hashCode(), info3.hashCode())

    assert(info1.toString != null)
  }
