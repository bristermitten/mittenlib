package me.bristermitten.mittenlib.config.names;

/**
 * Patterns for naming fields in a config.
 * This defines how the field name is transformed into a key for the config data.
 * For example, in Java we write {@code String helloWorld;} but our data may be stored as
 * {@code "hello_world": "Hello World!"}, or {@code "hello-world": "Hello World!"}, or {@code "helloWorld": "Hello World!"}.
 * Writing these in the source is often impossible, and doing it manually with {@link ConfigName} is tedious, so
 * we can use {@link NamingPattern} to automatically transform the field name into the key.
 */
public enum NamingPatterns {
    /**
     * Match the field name
     */
    DEFAULT,
    /**
     * lowerCamelCase. If you're following Java conventions
     * then this is identical to {@link NamingPatterns#DEFAULT}
     */
    LOWER_CAMEL_CASE,
    /**
     * UpperCamelCase / PascalCase
     */
    UPPER_CAMEL_CASE,
    /**
     * lower_snake_case
     */
    LOWER_SNAKE_CASE,
    /**
     * Upper_Snake_Case
     */
    UPPER_SNAKE_CASE,
    /**
     * lower-kebab-case
     */
    LOWER_KEBAB_CASE,
    /**
     * Upper-Kebab-Case
     */
    UPPER_KEBAB_CASE

}
