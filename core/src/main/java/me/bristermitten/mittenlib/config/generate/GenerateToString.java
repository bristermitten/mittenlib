package me.bristermitten.mittenlib.config.generate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Target;

/**
 * Instructs the annotation processor to generate a toString method for a generated config class.
 * This behaviour will cascade to inner classes (see {@link CascadeToInnerClasses})
 */
@Target(ElementType.TYPE)
@Inherited
@CascadeToInnerClasses
public @interface GenerateToString {
}
