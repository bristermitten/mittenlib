package me.bristermitten.mittenlib.collections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.AbstractMap;
import java.util.Collections;
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

    public static <K, V> @Unmodifiable Map<K, V> of(@NotNull K k1, @NotNull V v1, @NotNull K k2, @NotNull V v2, @NotNull K k3, @NotNull V v3) {
        return new MapImpls.Map3<>(k1, v1, k2, v2, k3, v3);
    }

    public static <K, V> @Unmodifiable Map<K, V> of(@NotNull K k1, @NotNull V v1, @NotNull K k2, @NotNull V v2, @NotNull K k3, @NotNull V v3, @NotNull K k4, @NotNull V v4) {
        return new MapImpls.MapN<>(
                Sets.of(
                        new AbstractMap.SimpleEntry<>(k1, v1),
                        new AbstractMap.SimpleEntry<>(k2, v2),
                        new AbstractMap.SimpleEntry<>(k3, v3),
                        new AbstractMap.SimpleEntry<>(k4, v4)
                )
        );
    }

    public static <K, V> @Unmodifiable Map<K, V> of(@NotNull K k1, @NotNull V v1, @NotNull K k2, @NotNull V v2, @NotNull K k3, @NotNull V v3, @NotNull K k4, @NotNull V v4, @NotNull K k5, @NotNull V v5) {
        return new MapImpls.MapN<>(
                Sets.of(
                        new AbstractMap.SimpleEntry<>(k1, v1),
                        new AbstractMap.SimpleEntry<>(k2, v2),
                        new AbstractMap.SimpleEntry<>(k3, v3),
                        new AbstractMap.SimpleEntry<>(k4, v4),
                        new AbstractMap.SimpleEntry<>(k5, v5)
                )
        );
    }

    @NotNull
    public static <K, V> @Unmodifiable Map<K, V> of(@NotNull K k1, @NotNull V v1, @NotNull K k2, @NotNull V v2, @NotNull K k3, @NotNull V v3, @NotNull K k4, @NotNull V v4, @NotNull K k5, @NotNull V v5, @NotNull K k6, @NotNull V v6) {
        return new MapImpls.MapN<>(
                Sets.of(
                        new AbstractMap.SimpleEntry<>(k1, v1),
                        new AbstractMap.SimpleEntry<>(k2, v2),
                        new AbstractMap.SimpleEntry<>(k3, v3),
                        new AbstractMap.SimpleEntry<>(k4, v4),
                        new AbstractMap.SimpleEntry<>(k5, v5),
                        new AbstractMap.SimpleEntry<>(k6, v6)
                )
        );
    }


    @NotNull
    public static <K, V> @Unmodifiable Map<K, V> of(@NotNull K k1, @NotNull V v1, @NotNull K k2, @NotNull V v2, @NotNull K k3, @NotNull V v3, @NotNull K k4, @NotNull V v4, @NotNull K k5, @NotNull V v5, @NotNull K k6, @NotNull V v6, @NotNull K k7, @NotNull V v7) {
        return new MapImpls.MapN<>(
                Sets.of(
                        new AbstractMap.SimpleEntry<>(k1, v1),
                        new AbstractMap.SimpleEntry<>(k2, v2),
                        new AbstractMap.SimpleEntry<>(k3, v3),
                        new AbstractMap.SimpleEntry<>(k4, v4),
                        new AbstractMap.SimpleEntry<>(k5, v5),
                        new AbstractMap.SimpleEntry<>(k6, v6),
                        new AbstractMap.SimpleEntry<>(k7, v7)
                )
        );
    }

    @NotNull
    public static <K, V> @Unmodifiable Map<K, V> of(@NotNull K k1, @NotNull V v1, @NotNull K k2, @NotNull V v2, @NotNull K k3, @NotNull V v3, @NotNull K k4, @NotNull V v4, @NotNull K k5, @NotNull V v5, @NotNull K k6, @NotNull V v6, @NotNull K k7, @NotNull V v7, @NotNull K k8, @NotNull V v8) {
        return new MapImpls.MapN<>(
                Sets.of(
                        new AbstractMap.SimpleEntry<>(k1, v1),
                        new AbstractMap.SimpleEntry<>(k2, v2),
                        new AbstractMap.SimpleEntry<>(k3, v3),
                        new AbstractMap.SimpleEntry<>(k4, v4),
                        new AbstractMap.SimpleEntry<>(k5, v5),
                        new AbstractMap.SimpleEntry<>(k6, v6),
                        new AbstractMap.SimpleEntry<>(k7, v7),
                        new AbstractMap.SimpleEntry<>(k8, v8)
                )
        );
    }

    @NotNull
    public static <K, V> @Unmodifiable Map<K, V> of(@NotNull K k1, @NotNull V v1, @NotNull K k2, @NotNull V v2, @NotNull K k3, @NotNull V v3, @NotNull K k4, @NotNull V v4, @NotNull K k5, @NotNull V v5, @NotNull K k6, @NotNull V v6, @NotNull K k7, @NotNull V v7, @NotNull K k8, @NotNull V v8, @NotNull K k9, @NotNull V v9) {
        return new MapImpls.MapN<>(
                Sets.of(
                        new AbstractMap.SimpleEntry<>(k1, v1),
                        new AbstractMap.SimpleEntry<>(k2, v2),
                        new AbstractMap.SimpleEntry<>(k3, v3),
                        new AbstractMap.SimpleEntry<>(k4, v4),
                        new AbstractMap.SimpleEntry<>(k5, v5),
                        new AbstractMap.SimpleEntry<>(k6, v6),
                        new AbstractMap.SimpleEntry<>(k7, v7),
                        new AbstractMap.SimpleEntry<>(k8, v8),
                        new AbstractMap.SimpleEntry<>(k9, v9)
                )
        );
    }

    @NotNull
    public static <K, V> @Unmodifiable Map<K, V> of(@NotNull K k1, @NotNull V v1, @NotNull K k2, @NotNull V v2, @NotNull K k3, @NotNull V v3, @NotNull K k4, @NotNull V v4, @NotNull K k5, @NotNull V v5, @NotNull K k6, @NotNull V v6, @NotNull K k7, @NotNull V v7, @NotNull K k8, @NotNull V v8, @NotNull K k9, @NotNull V v9, @NotNull K k10, @NotNull V v10) {
        return new MapImpls.MapN<>(
                Sets.of(
                        new AbstractMap.SimpleEntry<>(k1, v1),
                        new AbstractMap.SimpleEntry<>(k2, v2),
                        new AbstractMap.SimpleEntry<>(k3, v3),
                        new AbstractMap.SimpleEntry<>(k4, v4),
                        new AbstractMap.SimpleEntry<>(k5, v5),
                        new AbstractMap.SimpleEntry<>(k6, v6),
                        new AbstractMap.SimpleEntry<>(k7, v7),
                        new AbstractMap.SimpleEntry<>(k8, v8),
                        new AbstractMap.SimpleEntry<>(k9, v9),
                        new AbstractMap.SimpleEntry<>(k10, v10)
                )
        );
    }

    @SafeVarargs
    @NotNull
    public static <K, V> @Unmodifiable Map<K, V> of(@NotNull Map.Entry<K, V>... entries) {
        if (entries.length == 0) {
            return Collections.emptyMap();
        }
        if (entries.length == 1) {
            return of(entries[0].getKey(), entries[0].getValue());
        }

        return new MapImpls.MapN<>(Sets.of(entries));
    }

    @NotNull
    public static <K, V> Map.@Unmodifiable Entry<K, V> entry(@NotNull K key, @NotNull V value) {
        return new AbstractMap.SimpleImmutableEntry<>(key, value);
    }
}
