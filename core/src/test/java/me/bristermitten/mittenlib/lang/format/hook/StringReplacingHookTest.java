package me.bristermitten.mittenlib.lang.format.hook;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StringReplacingHookTest {

    @Test
    void shouldRegister() {
        var hook = new StringReplacingHook();
        assertTrue(hook.shouldRegister());
    }

    @Test
    void format() {
        var message = "Hello {name} {abcde} {abc}";
        var hook = new StringReplacingHook(
                "{name}", "Test",
                "{abcde}", "test2",
                "{abc}", null
        );

        var result = hook.format(message, null);
        assertEquals("Hello Test test2 null", result);
    }
}
