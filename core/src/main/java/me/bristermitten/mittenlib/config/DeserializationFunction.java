package me.bristermitten.mittenlib.config;

import me.bristermitten.mittenlib.util.Result;

import java.util.function.Function;

/**
 * A function that deserializes a config, taking a {@link DeserializationContext} and returning a {@link Result} of type {@link T}
 *
 * @param <T> the type to deserialize to
 */
@FunctionalInterface
public interface DeserializationFunction<T> extends Function<DeserializationContext, Result<T>> {
}
