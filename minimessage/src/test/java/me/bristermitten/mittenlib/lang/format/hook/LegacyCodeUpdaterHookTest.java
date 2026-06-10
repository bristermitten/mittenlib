package me.bristermitten.mittenlib.lang.format.hook;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LegacyCodeUpdaterHookTest {

    @Test
    void format() {
        LegacyCodeUpdaterHook hook = new LegacyCodeUpdaterHook();

        assertEquals("<red>Test <green>Message", hook.format("§cTest §aMessage", null));
        assertEquals("", hook.format("", null));
    }
}
