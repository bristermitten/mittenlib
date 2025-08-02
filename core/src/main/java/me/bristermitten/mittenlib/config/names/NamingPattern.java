package me.bristermitten.mittenlib.config.names;

import me.bristermitten.mittenlib.config.generate.CascadeToInnerClasses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Defines a naming pattern for a class or field.
 * See {@link NamingPatterns} for the available patterns and why they are useful.
 */
@Target({ElementType.TYPE, ElementType.FIELD})
@CascadeToInnerClasses
public @interface NamingPattern {
    /**
     * The naming pattern to use for the annotated class or field.
     * @return the naming pattern to use
     */
    NamingPatterns value() default NamingPatterns.DEFAULT;
}
