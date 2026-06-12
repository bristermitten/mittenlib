package me.bristermitten.mittenlib.annotations.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class ConfigProcessingExceptionTest {

    @Test
    void testConstructor() {
        Throwable cause = new RuntimeException("root cause");
        ConfigProcessingException exception = new ConfigProcessingException("error occurred", cause);

        assertEquals("error occurred", exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}
