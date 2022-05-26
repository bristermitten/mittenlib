package me.bristermitten.mittenlib.lang.format.hook;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LegacyCodeUpdaterHookTest {

    @Test
    void format() {
        LegacyCodeUpdaterHook hook = new LegacyCodeUpdaterHook();

        assertEquals("<red>Test <green>Message", hook.format("§cTest §aMessage", null));
        assertEquals("", hook.format("", null));
    }
}
