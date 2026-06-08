package me.bristermitten.mittenlib.config;

import me.bristermitten.mittenlib.config.generate.CascadeToInnerClasses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Target;

/**
 * Defines the parsing scheme for an enum value, i.e. how it should be parsed from a string.
 * Generated deserialization code will use this to determine how to parse the enum value.
 *
 * @see EnumParsingSchemes the scheme options.
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
@CascadeToInnerClasses
@Inherited
public @interface EnumParsingScheme {
    EnumParsingSchemes value();
}
