package me.bristermitten.mittenlib.annotations.ast

import org.mockito.Mockito.mock
import javax.lang.model.element.TypeElement

class CustomSerializerInfoTest extends munit.FunSuite:

  test("record methods") {
    val element1 = mock(classOf[TypeElement])
    val element2 = mock(classOf[TypeElement])

    val info1 = CustomSerializerInfo(element1, true)
    val info2 = CustomSerializerInfo(element1, true)
    val info3 = CustomSerializerInfo(element2, false)

    assertEquals(info1.serializerClass, element1)
    assertEquals(info1.isStatic, true)

    assertEquals(info1, info2)
    assertNotEquals(info1, info3)

    assertEquals(info1.hashCode(), info2.hashCode())
    assertNotEquals(info1.hashCode(), info3.hashCode())

    assert(info1.toString != null)
  }
