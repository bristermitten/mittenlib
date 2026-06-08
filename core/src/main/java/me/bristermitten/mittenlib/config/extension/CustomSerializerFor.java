package me.bristermitten.mittenlib.config.extension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a custom serializer for the specified target type. The annotated class must
 * implement {@link CustomSerializer} or contain a static method with the signature:
 *
 * <pre>
 * public static DataTree serialize(TargetType value, SerializationContext context)
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CustomSerializerFor {
    Class<?> value();
}
