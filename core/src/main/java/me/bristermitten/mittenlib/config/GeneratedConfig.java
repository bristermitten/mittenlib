package me.bristermitten.mittenlib.config;

/**
 * Marks a class as being generated from a config DTO.
 * See the annotation-processor module for more information
 */
public @interface GeneratedConfig {
    /**
     * The {@link Config} class that was used to generate this type.
     *
     * @return The Config class
     */
    Class<?> source();
}
