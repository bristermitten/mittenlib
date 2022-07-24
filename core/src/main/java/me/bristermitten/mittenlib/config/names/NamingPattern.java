package me.bristermitten.mittenlib.config.names;

import me.bristermitten.mittenlib.config.generate.CascadeToInnerClasses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.FIELD})
@CascadeToInnerClasses
public @interface NamingPattern {
    NamingPatterns value() default NamingPatterns.DEFAULT;
}
