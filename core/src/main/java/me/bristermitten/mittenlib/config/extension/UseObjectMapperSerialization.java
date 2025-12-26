package me.bristermitten.mittenlib.config.extension;

import me.bristermitten.mittenlib.config.Config;
import me.bristermitten.mittenlib.config.generate.CascadeToInnerClasses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a configuration property to use ObjectMapper for serialization.
 * This is an explicit opt-in for types that are not natively supported by the serialization system.
 * <p>
 * By default, only the following types are supported for serialization:
 * <ul>
 *   <li>Primitives and their boxed versions</li>
 *   <li>String</li>
 *   <li>Enums</li>
 *   <li>@{@link Config} annotated types</li>
 *   <li>List and Map of supported types</li>
 * </ul>
 * <p>
 * For any other type, you must explicitly annotate the property with this annotation.
 * The ObjectMapper will handle the serialization using its generic mapping mechanism.
 * <p>
 * Example:
 * <pre>
 * &#64;Config
 * public class MyConfigDTO {
 *     &#64;UseObjectMapperSerialization
 *     CustomType myCustomProperty;
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
@CascadeToInnerClasses
public @interface UseObjectMapperSerialization {
}
