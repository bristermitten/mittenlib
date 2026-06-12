package me.bristermitten.mittenlib.annotations.util;

import static org.junit.jupiter.api.Assertions.*;

import me.bristermitten.mittenlib.config.names.ConfigName;
import org.junit.jupiter.api.Test;

class PrivateAnnotationsTest {

    @Test
    void testIsPrivate() {
        assertTrue(PrivateAnnotations.isPrivate(ConfigName.class.getName()));
        assertFalse(PrivateAnnotations.isPrivate("some.other.Annotation"));
    }
}
