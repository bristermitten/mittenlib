package me.bristermitten.mittenlib.config.names;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Marks an expected name for a field in a config file.
 * If this annotation is present, the {@link #value()} will be used instead of the generated name from the
 * {@link NamingPattern}.
 */
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface ConfigName {
    /**
     * The expected name of the field / key in the config file.
     * @return The expected name of the field / key in the config file
     */
    String value();
}
