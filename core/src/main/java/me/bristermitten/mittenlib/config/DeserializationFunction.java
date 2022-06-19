package me.bristermitten.mittenlib.config;

import me.bristermitten.mittenlib.util.Result;

import java.util.function.Function;

@FunctionalInterface
public interface DeserializationFunction<T> extends Function<DeserializationContext, Result<T>> {
}
