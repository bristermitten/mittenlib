package me.bristermitten.mittenlib.config.generate;

import java.lang.annotation.*;

/**
 * If an Annotation should also apply to inner / nested classes.
 * This is similar to {@link Inherited} but doesn't require a sub/superclass relation,
 * which better suits the patterns often used in config DTO types.
 * <p>
 * In other words, if an annotation marked with {@link CascadeToInnerClasses} is present on a class, the same annotation will also be
 * treated as present on any inner classes.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface CascadeToInnerClasses {

}
