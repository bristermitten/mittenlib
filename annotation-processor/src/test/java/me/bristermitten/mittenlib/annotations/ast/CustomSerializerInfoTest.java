package me.bristermitten.mittenlib.annotations.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;

import javax.lang.model.element.TypeElement;
import org.junit.jupiter.api.Test;

class CustomSerializerInfoTest {

    @Test
    void testRecordMethods() {
        TypeElement element1 = mock(TypeElement.class);
        TypeElement element2 = mock(TypeElement.class);

        CustomSerializerInfo info1 = new CustomSerializerInfo(element1, true);
        CustomSerializerInfo info2 = new CustomSerializerInfo(element1, true);
        CustomSerializerInfo info3 = new CustomSerializerInfo(element2, false);

        assertEquals(element1, info1.serializerClass());
        assertEquals(true, info1.isStatic());

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
