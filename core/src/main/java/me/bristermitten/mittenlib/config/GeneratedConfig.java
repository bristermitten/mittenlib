package me.bristermitten.mittenlib.config;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks a class as being generated from a config DTO.
 * <b>You probably should not use this annotation!</b>
 * <p>
 *
 * @apiNote See the annotation-processor module for more information
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface GeneratedConfig {
    /**
     * The {@link Config} class that was used to generate this type.
     *
     * @return The Config class
     */
    Class<?> source();

    /**
     * Whether the config is dynamically initializable, i.e., can be synthesized with some default values if the config file doesn't exist.
     * This usually requires the config to have an explicitly declared default value for every non-nullable property.
     */
    boolean isDynamicallyInitializable() default false;

    /**
     * The names of properties that prevent this config from being dynamically initializable.
     * These are properties that are required (non-nullable) and do not have a default value.
     *
     * @return The names of properties that prevent dynamic initialization
     */
    String[] unserializableProperties() default {};
}
