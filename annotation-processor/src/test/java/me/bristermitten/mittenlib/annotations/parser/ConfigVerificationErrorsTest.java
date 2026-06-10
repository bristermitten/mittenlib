package me.bristermitten.mittenlib.annotations.parser;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class ConfigVerificationErrorsTest {

    @Test
    void testConstructor() {
        assertNotNull(new ConfigVerificationErrors());
    }
}
