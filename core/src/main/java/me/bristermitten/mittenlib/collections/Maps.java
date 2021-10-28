package me.bristermitten.mittenlib.collections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Map;

/**
 * Utility functions for creating immutable maps
 * Unnecessary in Java 9+
 */
public class Maps {
    private Maps() {
    }

    public static <K, V> @Unmodifiable Map<K, V> of(@NotNull K k, @NotNull V v) {
        return new MapImpls.Map1<>(k, v);
    }

    public static <K, V> @Unmodifiable Map<K, V> of(@NotNull K k1, @NotNull V v1, @NotNull K k2, @NotNull V v2) {
        return new MapImpls.Map2<>(k1, v1, k2, v2);
    }
}
