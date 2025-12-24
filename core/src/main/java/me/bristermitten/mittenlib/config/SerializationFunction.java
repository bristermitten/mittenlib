package me.bristermitten.mittenlib.config;

import me.bristermitten.mittenlib.config.tree.DataTree;

import java.util.function.Function;

/**
 * A function that serializes a config object to a {@link DataTree}
 *
 * @param <T> the type to serialize from
 */
@FunctionalInterface
public interface SerializationFunction<T> extends Function<T, DataTree> {
}
