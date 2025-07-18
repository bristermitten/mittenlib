package me.bristermitten.mittenlib.collections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility functions for creating immutable maps
 * Unnecessary in Java 9+
 */
public class Maps {
    private Maps() {
    }

    public static <K, V> @Unmodifiable MLImmutableMap<K, V> empty() {
        return new MapImpls.Map0<>();
    }

    public static <K, V> @Unmodifiable MLImmutableMap<K, V> of() {
        return empty();
    }

    public static <K, V> @Unmodifiable MLImmutableMap<K, V> of(@NotNull K k, @NotNull V v) {
        return new MapImpls.Map1<>(k, v);
    }

    public static <K, V> @Unmodifiable MLImmutableMap<K, V> of(@NotNull K k1, @NotNull V v1, @NotNull K k2, @NotNull V v2) {
        return new MapImpls.Map2<>(k1, v1, k2, v2);
    }

    public static <K, V> @Unmodifiable MLImmutableMap<K, V> of(@NotNull K k1, @NotNull V v1, @NotNull K k2, @NotNull V v2, @NotNull K k3, @NotNull V v3) {
        return new MapImpls.Map3<>(k1, v1, k2, v2, k3, v3);
    }

    public static <K, V> @Unmodifiable MLImmutableMap<K, V> of(@NotNull K k1, @NotNull V v1, @NotNull K k2, @NotNull V v2, @NotNull K k3, @NotNull V v3, @NotNull K k4, @NotNull V v4) {
        return new MapImpls.MapN<>(
                Sets.of(
                        entry(k1, v1),
                        entry(k2, v2),
                        entry(k3, v3),
                        entry(k4, v4)
                )
        );
    }

    public static <K, V> @Unmodifiable MLImmutableMap<K, V> of(@NotNull K k1, @NotNull V v1, @NotNull K k2, @NotNull V v2, @NotNull K k3, @NotNull V v3, @NotNull K k4, @NotNull V v4, @NotNull K k5, @NotNull V v5) {
        return new MapImpls.MapN<>(
                Sets.of(
                        entry(k1, v1),
                        entry(k2, v2),
                        entry(k3, v3),
                        entry(k4, v4),
                        entry(k5, v5)
                )
        );
    }

    @NotNull
    public static <K, V> @Unmodifiable MLImmutableMap<K, V> of(@NotNull K k1, @NotNull V v1, @NotNull K k2, @NotNull V v2, @NotNull K k3, @NotNull V v3, @NotNull K k4, @NotNull V v4, @NotNull K k5, @NotNull V v5, @NotNull K k6, @NotNull V v6) {
        return new MapImpls.MapN<>(
                Sets.of(
                        entry(k1, v1),
                        entry(k2, v2),
                        entry(k3, v3),
                        entry(k4, v4),
                        entry(k5, v5),
                        entry(k6, v6)
                )
        );
    }


    @NotNull
    public static <K, V> @Unmodifiable MLImmutableMap<K, V> of(@NotNull K k1, @NotNull V v1, @NotNull K k2, @NotNull V v2, @NotNull K k3, @NotNull V v3, @NotNull K k4, @NotNull V v4, @NotNull K k5, @NotNull V v5, @NotNull K k6, @NotNull V v6, @NotNull K k7, @NotNull V v7) {
        return new MapImpls.MapN<>(
                Sets.of(
                        entry(k1, v1),
                        entry(k2, v2),
                        entry(k3, v3),
                        entry(k4, v4),
                        entry(k5, v5),
                        entry(k6, v6),
                        entry(k7, v7)
                )
        );
    }

    @NotNull
    public static <K, V> @Unmodifiable MLImmutableMap<K, V> of(@NotNull K k1, @NotNull V v1, @NotNull K k2, @NotNull V v2, @NotNull K k3, @NotNull V v3, @NotNull K k4, @NotNull V v4, @NotNull K k5, @NotNull V v5, @NotNull K k6, @NotNull V v6, @NotNull K k7, @NotNull V v7, @NotNull K k8, @NotNull V v8) {
        return new MapImpls.MapN<>(
                Sets.of(
                        entry(k1, v1),
                        entry(k2, v2),
                        entry(k3, v3),
                        entry(k4, v4),
                        entry(k5, v5),
                        entry(k6, v6),
                        entry(k7, v7),
                        entry(k8, v8)
                )
        );
    }

    @NotNull
    public static <K, V> @Unmodifiable MLImmutableMap<K, V> of(@NotNull K k1, @NotNull V v1, @NotNull K k2, @NotNull V v2, @NotNull K k3, @NotNull V v3, @NotNull K k4, @NotNull V v4, @NotNull K k5, @NotNull V v5, @NotNull K k6, @NotNull V v6, @NotNull K k7, @NotNull V v7, @NotNull K k8, @NotNull V v8, @NotNull K k9, @NotNull V v9) {
        return new MapImpls.MapN<>(
                Sets.of(
                        entry(k1, v1),
                        entry(k2, v2),
                        entry(k3, v3),
                        entry(k4, v4),
                        entry(k5, v5),
                        entry(k6, v6),
                        entry(k7, v7),
                        entry(k8, v8),
                        entry(k9, v9)
                )
        );
    }

    @NotNull
    public static <K, V> @Unmodifiable MLImmutableMap<K, V> of(@NotNull K k1, @NotNull V v1, @NotNull K k2, @NotNull V v2, @NotNull K k3, @NotNull V v3, @NotNull K k4, @NotNull V v4, @NotNull K k5, @NotNull V v5, @NotNull K k6, @NotNull V v6, @NotNull K k7, @NotNull V v7, @NotNull K k8, @NotNull V v8, @NotNull K k9, @NotNull V v9, @NotNull K k10, @NotNull V v10) {
        return new MapImpls.MapN<>(
                Sets.of(
                        entry(k1, v1),
                        entry(k2, v2),
                        entry(k3, v3),
                        entry(k4, v4),
                        entry(k5, v5),
                        entry(k6, v6),
                        entry(k7, v7),
                        entry(k8, v8),
                        entry(k9, v9),
                        entry(k10, v10)
                )
        );
    }

    /*
    Copies
     */

    /**
     * Creates a copy of the given map with the specified key and value added.
     *
     * @param map the map to copy
     * @param k   the key to add
     * @param v   the value to add at the specified key
     * @param <K> the type of keys in the map
     * @param <V> the type of values in the map
     * @return a new map containing all entries from the original map plus the new key-value pair
     */
    public static @NotNull <K, V> MLImmutableMap<K, V> of(@NotNull Map<K, V> map, @NotNull K k, @NotNull V v) {
        if (map instanceof MLImmutableMap) {
            return ((MLImmutableMap<K, V>) map).plus(k, v);
        }
        return new MapImpls.MapN<>(Sets.ofAll(map.entrySet()).plus(entry(k, v)));
    }

    @SafeVarargs
    @NotNull
    public static <K, V> @Unmodifiable MLImmutableMap<K, V> of(@NotNull Map.Entry<K, V>... entries) {
        if (entries.length == 0) {
            return empty();
        }
        if (entries.length == 1) {
            return of(entries[0].getKey(), entries[0].getValue());
        }

        return new MapImpls.MapN<>(Sets.of(entries));
    }

    public static <K, V> @Unmodifiable @NotNull MLImmutableMap<K, V> of(@NotNull Collection<Map.Entry<K, V>> entries) {
        if (entries.isEmpty()) {
            return empty();
        }
        if (entries.size() == 1) {
            Map.Entry<K, V> entry = entries.iterator().next();
            return of(entry.getKey(), entry.getValue());
        }

        Set<Map.Entry<K, V>> collect = entries
                .stream()
                .map(entry -> entry(entry.getKey(), entry.getValue()))
                .collect(Collectors.toSet());
        return new MapImpls.MapN<>(Sets.ofAll(collect));
    }

    @NotNull
    public static <K, V> Map.@Unmodifiable Entry<K, V> entry(@NotNull K key, @NotNull V value) {
        return new MapImpls.MLEntry<>(key, value);
    }
}
