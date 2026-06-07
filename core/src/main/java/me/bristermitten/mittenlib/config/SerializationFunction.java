package me.bristermitten.mittenlib.config;

import me.bristermitten.mittenlib.config.tree.DataTree;

import java.util.function.BiFunction;

/**
 * A function that serializes a config object to a {@link DataTree}
 *
 * @param <T> the type to serialize from
 */
@FunctionalInterface
public interface SerializationFunction<T> extends BiFunction<T, SerializationContext, DataTree> {
    /**
     * Generate a default {@link DataTree} for this type.
     * This is used when a config file does not exist to create a default one.
     *
     * @param context the context to use for serialization
     * @return a default DataTree
     */
    default DataTree generateDefault(SerializationContext context) {
        return DataTree.null_();
    }
}
