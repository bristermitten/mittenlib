package me.bristermitten.mittenlib.config;

import me.bristermitten.mittenlib.config.reader.ObjectMapper;
import me.bristermitten.mittenlib.config.tree.DataTree;

import java.util.function.BiFunction;

/**
 * A function that serializes a config object to a {@link DataTree}
 *
 * @param <T> the type to serialize from
 */
@FunctionalInterface
public interface SerializationFunction<T> extends BiFunction<T, ObjectMapper, DataTree> {
}
