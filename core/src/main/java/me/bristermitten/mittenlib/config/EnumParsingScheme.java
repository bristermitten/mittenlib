package me.bristermitten.mittenlib.config;

import me.bristermitten.mittenlib.config.generate.CascadeToInnerClasses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
@CascadeToInnerClasses
@Inherited
public @interface EnumParsingScheme {
    EnumParsingSchemes value();
}
