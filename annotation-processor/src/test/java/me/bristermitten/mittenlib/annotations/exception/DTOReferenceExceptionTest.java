package me.bristermitten.mittenlib.annotations.exception;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import me.bristermitten.mittenlib.annotations.compile.GeneratedTypeCache;
import org.junit.jupiter.api.Test;

class DTOReferenceExceptionTest {

    @Test
    void testGetMessageWithReplaceWithNonNull() {
        TypeMirror typeUsed = mock(TypeMirror.class);
        GeneratedTypeCache typeCache = mock(GeneratedTypeCache.class);
        Element source = mock(Element.class);

        when(typeUsed.toString()).thenReturn("SomeInvalidType");
        when(source.toString()).thenReturn("SomeSourceElement");

        DTOReferenceException exception = new DTOReferenceException(typeUsed, typeCache, String.class, source);

        String message = exception.getMessage();
        assertTrue(message.contains("java.lang.String"));
        assertTrue(message.contains("SomeInvalidType"));
        assertTrue(message.contains("SomeSourceElement"));
    }

    @Test
    void testGetMessageWithReplaceWithNullAndEmptyCache() {
        TypeMirror typeUsed = mock(TypeMirror.class);
        GeneratedTypeCache typeCache = mock(GeneratedTypeCache.class);

        when(typeUsed.toString()).thenReturn("SomeInvalidType");
        when(typeCache.getByName("SomeInvalidType")).thenReturn(Collections.emptySet());

        DTOReferenceException exception = new DTOReferenceException(typeUsed, typeCache, null, null);

        String message = exception.getMessage();
        assertTrue(message.contains("Unknown type SomeInvalidType"));
    }

    @Test
    void testGetMessageWithReplaceWithNullAndSingleCacheHit() {
        TypeMirror typeUsed = mock(TypeMirror.class);
        GeneratedTypeCache typeCache = mock(GeneratedTypeCache.class);
        TypeElement cachedElement = mock(TypeElement.class);

        when(typeUsed.toString()).thenReturn("SomeInvalidType");
        when(cachedElement.toString()).thenReturn("CachedType");
        when(typeCache.getByName("SomeInvalidType")).thenReturn(Collections.singleton(cachedElement));

        DTOReferenceException exception = new DTOReferenceException(typeUsed, typeCache, null, null);

        String message = exception.getMessage();
        assertTrue(message.contains("CachedType"));
        assertTrue(message.contains("Unknown Location"));
    }

    @Test
    void testGetMessageWithReplaceWithNullAndMultipleCacheHits() {
        TypeMirror typeUsed = mock(TypeMirror.class);
        GeneratedTypeCache typeCache = mock(GeneratedTypeCache.class);
        TypeElement cachedElement1 = mock(TypeElement.class);
        TypeElement cachedElement2 = mock(TypeElement.class);

        when(typeUsed.toString()).thenReturn("SomeInvalidType");
        when(cachedElement1.toString()).thenReturn("CachedType1");
        when(cachedElement2.toString()).thenReturn("CachedType2");

        Set<TypeElement> cachedElements = new LinkedHashSet<>();
        cachedElements.add(cachedElement1);
        cachedElements.add(cachedElement2);
        when(typeCache.getByName("SomeInvalidType")).thenReturn(cachedElements);

        DTOReferenceException exception = new DTOReferenceException(typeUsed, typeCache, null, null);

        String message = exception.getMessage();
        assertTrue(message.contains("any of [CachedType1, CachedType2]"));
    }
}
