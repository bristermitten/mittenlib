package me.bristermitten.mittenlib.config.extension;

import me.bristermitten.mittenlib.config.SerializationFunction;

/**
 * Defines a custom serializer for a specific type.
 * Implementations should implement this interface and be annotated with {@link CustomSerializerFor}.
 * The serializer can then be registered and automatically injected into configuration savers.
 *
 * @param <T> the type that this serializer handles
 */
public interface CustomSerializer<T> extends SerializationFunction<T> {
}
