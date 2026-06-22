package me.bristermitten.mittenlib.annotations.exception

import org.mockito.Mockito.*
import javax.lang.model.element.{Element, TypeElement}
import javax.lang.model.`type`.TypeMirror
import me.bristermitten.mittenlib.annotations.compile.GeneratedTypeCache
import java.util.Collections

class DTOReferenceExceptionTest extends munit.FunSuite:

  test("getMessage with replaceWith non-null") {
    val typeUsed = mock(classOf[TypeMirror])
    val typeCache = mock(classOf[GeneratedTypeCache])
    val source = mock(classOf[Element])

    when(typeUsed.toString).thenReturn("SomeInvalidType")
    when(source.toString).thenReturn("SomeSourceElement")

    val exception =
      new DTOReferenceException(typeUsed, typeCache, classOf[String], source)

    val message = exception.getMessage
    assert(message.contains("java.lang.String"))
    assert(message.contains("SomeInvalidType"))
    assert(message.contains("SomeSourceElement"))
  }

  test("getMessage with replaceWith null and empty cache") {
    val typeUsed = mock(classOf[TypeMirror])
    val typeCache = mock(classOf[GeneratedTypeCache])

    when(typeUsed.toString).thenReturn("SomeInvalidType")
    when(typeCache.getByName("SomeInvalidType"))
      .thenReturn(Collections.emptySet())

    val exception = new DTOReferenceException(typeUsed, typeCache, null, null)

    val message = exception.getMessage
    assert(message.contains("Unknown type SomeInvalidType"))
  }

  test("getMessage with replaceWith null and single cache hit") {
    val typeUsed = mock(classOf[TypeMirror])
    val typeCache = mock(classOf[GeneratedTypeCache])
    val cachedElement = mock(classOf[TypeElement])

    when(typeUsed.toString).thenReturn("SomeInvalidType")
    when(cachedElement.toString).thenReturn("CachedType")
    when(typeCache.getByName("SomeInvalidType"))
      .thenReturn(Collections.singleton(cachedElement))

    val exception = new DTOReferenceException(typeUsed, typeCache, null, null)

    val message = exception.getMessage
    assert(message.contains("CachedType"))
    assert(message.contains("Unknown Location"))
  }

  test("getMessage with replaceWith null and multiple cache hits") {
    val typeUsed = mock(classOf[TypeMirror])
    val typeCache = mock(classOf[GeneratedTypeCache])
    val cachedElement1 = mock(classOf[TypeElement])
    val cachedElement2 = mock(classOf[TypeElement])

    when(typeUsed.toString).thenReturn("SomeInvalidType")
    when(cachedElement1.toString).thenReturn("CachedType1")
    when(cachedElement2.toString).thenReturn("CachedType2")

    val cachedElements = new java.util.LinkedHashSet[TypeElement]()
    cachedElements.add(cachedElement1)
    cachedElements.add(cachedElement2)
    when(typeCache.getByName("SomeInvalidType")).thenReturn(cachedElements)

    val exception = new DTOReferenceException(typeUsed, typeCache, null, null)

    val message = exception.getMessage
    assert(message.contains("any of [CachedType1, CachedType2]"))
  }
