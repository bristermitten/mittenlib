package me.bristermitten.mittenlib.lang.format.hook;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HexColorFixerHookTest {

    @Test
    void format() {
        final String format = new HexColorFixerHook()
                .format("hello §x§f§f§f§f§f§f world", null);
        assertEquals("hello <#ffffff> world", format);
    }

    @Test
    void format_normalString() {
        final String format = new HexColorFixerHook()
                .format("hello world", null);
        assertEquals("hello world", format);
    }
}
