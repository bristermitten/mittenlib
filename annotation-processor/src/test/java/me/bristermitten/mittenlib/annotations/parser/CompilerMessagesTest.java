package me.bristermitten.mittenlib.annotations.parser;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CompilerMessagesTest {

    @Test
    void testConfigClassParserCompilerMessages() {
        ConfigClassParserCompilerMessages[] values = ConfigClassParserCompilerMessages.values();
        assertTrue(values.length > 0);

        ConfigClassParserCompilerMessages val = ConfigClassParserCompilerMessages.valueOf(values[0].name());
        assertNotNull(val);
    }

    @Test
    void testCustomDeserializersCompilerMessages() {
        CustomDeserializersCompilerMessages[] values = CustomDeserializersCompilerMessages.values();
        assertTrue(values.length > 0);

        CustomDeserializersCompilerMessages val = CustomDeserializersCompilerMessages.valueOf(values[0].name());
        assertNotNull(val);
    }
}
