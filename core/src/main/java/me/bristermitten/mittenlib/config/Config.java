package me.bristermitten.mittenlib.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a type as a <a href="https://github.com/knightzmc/mittenlib/tree/master/annotation-processor#naming">DTO type</a>
 * that will be processed by the annotation processor, if present.
 * Without the annotation processor, this annotation has no effect.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Config {

    /**
     * The class name of the generated class.
     * If empty, the name will be generated from the DTO type's name
     *
     * @return The class name of the generated class
     */
    String className() default "";
}
