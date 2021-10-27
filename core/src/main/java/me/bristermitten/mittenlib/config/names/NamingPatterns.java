package me.bristermitten.mittenlib.config.names;

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
     * UPPER_SNAKE_CASE
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
