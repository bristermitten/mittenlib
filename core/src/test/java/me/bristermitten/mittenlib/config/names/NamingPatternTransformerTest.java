package me.bristermitten.mittenlib.config.names;

import org.junit.jupiter.api.Test;

import static me.bristermitten.mittenlib.config.names.NamingPatternTransformer.format;
import static me.bristermitten.mittenlib.config.names.NamingPatterns.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NamingPatternTransformerTest {

    @Test
    void assertThat_defaultPattern_isIdentity() {
        assertEquals("", format("", NamingPatterns.DEFAULT));
        assertEquals("a", format("a", NamingPatterns.DEFAULT));
        assertEquals("abcde", format("abcde", NamingPatterns.DEFAULT));
    }

    @Test
    void assertThat_lowerCamelCase_isIdentity() {
        assertEquals("", format("", LOWER_CAMEL_CASE));
        assertEquals("helloWorld", format("helloWorld", LOWER_CAMEL_CASE));
        assertEquals("hello", format("hello", LOWER_CAMEL_CASE));
    }

    @Test
    void assertThat_UpperCamelCase_works() {
        assertEquals("", format("", UPPER_CAMEL_CASE));
        assertEquals("HelloWorld", format("helloWorld", UPPER_CAMEL_CASE));
        assertEquals("Hello", format("hello", UPPER_CAMEL_CASE));
    }

    @Test
    void assertThat_lowerSnakeCase_works() {
        assertEquals("", format("", LOWER_SNAKE_CASE));
        assertEquals("hello_world", format("helloWorld", LOWER_SNAKE_CASE));
        assertEquals("hello", format("hello", LOWER_SNAKE_CASE));
    }

    @Test
    void assertThat_upperSnakeCase_works() {
        assertEquals("", format("", UPPER_SNAKE_CASE));
        assertEquals("Hello_World", format("helloWorld", UPPER_SNAKE_CASE));
        assertEquals("Hello", format("hello", UPPER_SNAKE_CASE));
    }

    @Test
    void assertThat_lowerKebabCase_works() {
        assertEquals("", format("", LOWER_KEBAB_CASE));
        assertEquals("hello-world", format("helloWorld", LOWER_KEBAB_CASE));
        assertEquals("hello", format("hello", LOWER_KEBAB_CASE));
    }

    @Test
    void assertThat_upperKebabCase_works() {
        assertEquals("", format("", UPPER_KEBAB_CASE));
        assertEquals("Hello-World", format("helloWorld", UPPER_KEBAB_CASE));
        assertEquals("Hello", format("hello", UPPER_KEBAB_CASE));
    }
}
