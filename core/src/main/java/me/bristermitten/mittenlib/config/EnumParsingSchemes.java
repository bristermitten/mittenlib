package me.bristermitten.mittenlib.config;

/**
 * Strategies to parse an enum from a string value. This affects how deserialization code is
 * generated.
 *
 * @see EnumParsingScheme
 */
public enum EnumParsingSchemes {
    /**
     * Requires the name to exactly match, i.e., under {@link String#equals(Object)}. The default
     * value.
     */
    EXACT_MATCH,
    /**
     * Requires the name to exactly match, but case insensitively, i.e. {@link
     * String#equalsIgnoreCase(String)}.
     */
    CASE_INSENSITIVE,
}
