package me.bristermitten.mittenlib.annotations.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import javax.lang.model.element.TypeElement;
import org.junit.jupiter.api.Test;

class CustomDeserializerInfoTest {

    @Test
    void testRecordMethods() {
        TypeElement element1 = mock(TypeElement.class);
        TypeElement element2 = mock(TypeElement.class);

        CustomDeserializerInfo info1 = new CustomDeserializerInfo(element1, true, false, true);
        CustomDeserializerInfo info2 = new CustomDeserializerInfo(element1, true, false, true);
        CustomDeserializerInfo info3 = new CustomDeserializerInfo(element2, false, true, false);

        assertEquals(element1, info1.deserializerClass());
        assertTrue(info1.isStatic());
        assertFalse(info1.isFallback());
        assertTrue(info1.isGlobal());

        assertEquals(info1, info2);
        assertNotEquals(info1, info3);

        assertEquals(info1.hashCode(), info2.hashCode());
        assertNotEquals(info1.hashCode(), info3.hashCode());

        assertNotNull(info1.toString());
    }

    private void assertNotNull(String str) {
        if (str == null) {
            throw new AssertionError("Expected non-null string");
        }
    }
}
