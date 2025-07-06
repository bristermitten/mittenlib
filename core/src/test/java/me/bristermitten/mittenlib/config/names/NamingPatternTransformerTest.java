package me.bristermitten.mittenlib.config.names;

import org.junit.jupiter.api.Test;

import static me.bristermitten.mittenlib.config.names.NamingPatternTransformer.format;
import static me.bristermitten.mittenlib.config.names.NamingPatterns.*;
import static org.assertj.core.api.Assertions.assertThat;

class NamingPatternTransformerTest {

    @Test
    void assertThat_defaultPattern_isIdentity() {
        assertThat(format("", DEFAULT)).isEqualTo("");
        assertThat(format("hello", DEFAULT)).isEqualTo("hello");
    }

    @Test
    void assertThat_lowerCamelCase_isIdentity() {
        assertThat(format("", LOWER_CAMEL_CASE)).isEqualTo("");
        assertThat(format("helloWorld", LOWER_CAMEL_CASE)).isEqualTo("helloWorld");
        assertThat(format("hello", LOWER_CAMEL_CASE)).isEqualTo("hello");
    }

    @Test
    void assertThat_UpperCamelCase_works() {
        assertThat(format("", UPPER_CAMEL_CASE)).isEqualTo("");
        assertThat(format("helloWorld", UPPER_CAMEL_CASE)).isEqualTo("HelloWorld");
        assertThat(format("hello", UPPER_CAMEL_CASE)).isEqualTo("Hello");
    }

    @Test
    void assertThat_lowerSnakeCase_works() {
        assertThat(format("", LOWER_SNAKE_CASE)).isEqualTo("");
        assertThat(format("helloWorld", LOWER_SNAKE_CASE)).isEqualTo("hello_world");
        assertThat(format("hello", LOWER_SNAKE_CASE)).isEqualTo("hello");
    }

    @Test
    void assertThat_upperSnakeCase_works() {
        assertThat(format("", UPPER_SNAKE_CASE)).isEqualTo("");
        assertThat(format("helloWorld", UPPER_SNAKE_CASE)).isEqualTo("Hello_World");
        assertThat(format("hello", UPPER_SNAKE_CASE)).isEqualTo("Hello");
    }

    @Test
    void assertThat_lowerKebabCase_works() {
        assertThat(format("", LOWER_KEBAB_CASE)).isEqualTo("");
        assertThat(format("helloWorld", LOWER_KEBAB_CASE)).isEqualTo("hello-world");
        assertThat(format("hello", LOWER_KEBAB_CASE)).isEqualTo("hello");
    }

    @Test
    void assertThat_upperKebabCase_works() {
        assertThat(format("", UPPER_KEBAB_CASE)).isEqualTo("");
        assertThat(format("helloWorld", UPPER_KEBAB_CASE)).isEqualTo("Hello-World");
        assertThat(format("hello", UPPER_KEBAB_CASE)).isEqualTo("Hello");
    }
}