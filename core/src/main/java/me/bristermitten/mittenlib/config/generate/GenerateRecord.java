package me.bristermitten.mittenlib.config.generate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Target;

/**
 * Instructs the annotation processor to generate a record rather than a normal class for a config.
 * Obviously, requires compiling against Java 17+
 * <p>
 * This behaviour will cascade to inner classes (see {@link CascadeToInnerClasses})
 */
@Target(ElementType.TYPE)
@Inherited
@CascadeToInnerClasses
public @interface GenerateRecord {
    boolean value() default true;
}
