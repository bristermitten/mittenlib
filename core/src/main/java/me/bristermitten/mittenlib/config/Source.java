package me.bristermitten.mittenlib.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@link Config} class as being loaded from a file.
 * The presence of this annotation means that a static final {@link Configuration }field named {@code CONFIG} is generated.
 * See library documentation for more information.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Source {
    /**
     * @return File name for the source of the config
     */
    String value();
}
