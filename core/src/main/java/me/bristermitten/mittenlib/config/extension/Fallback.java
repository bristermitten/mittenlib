package me.bristermitten.mittenlib.config.extension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a custom deserializer as a fallback deserializer.
 * If marked as a fallback, we will try as much of the normal deserialization logic as possible,
 * only calling the custom function when nothing else works.
 * Otherwise, we only try the custom function.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Fallback {
}
