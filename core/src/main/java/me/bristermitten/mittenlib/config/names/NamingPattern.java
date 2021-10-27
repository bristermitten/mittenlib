package me.bristermitten.mittenlib.config.names;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.FIELD})
public @interface NamingPattern {
    NamingPatterns value() default NamingPatterns.DEFAULT;
}
