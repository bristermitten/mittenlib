package me.bristermitten.mittenlib.config.generate;

import java.lang.annotation.*;

/**
 * If an Annotation should also apply to inner / nested classes
 * <p>
 * This is similar to {@link Inherited} but doesn't require a sub/superclass relation,
 * which better suits the patterns often used in config DTO types.
 * <p>
 * This is generally for internal use as a marker rather than to change behaviour.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface CascadeToInnerClasses {

}
