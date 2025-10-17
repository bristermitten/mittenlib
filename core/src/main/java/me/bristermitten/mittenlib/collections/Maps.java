package me.bristermitten.mittenlib.collections;


import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility functions for creating immutable maps
 * Unnecessary in Java 9+
 */
@NullMarked
public class Maps {
    private Maps() {
    }

    public static <K, V> @Unmodifiable MLImmutableMap<K, V> empty() {
        return new MapImpls.Map0<>();
    }

    public static <K, V> @Unmodifiable MLImmutableMap<K, V> of() {
        return empty();
    }

    public static <K, V> @Unmodifiable MLImmutableMap<K, V> of(@NonNull K k, @NonNull V v) {
        Objects.requireNonNull(k);
        Objects.requireNonNull(v);
        return new MapImpls.Map1<>(k, v);
    }

    public static <K, V> @Unmodifiable MLImmutableMap<K, V> of(@NonNull K k1,
                                                               @NonNull V v1,
                                                               @NonNull K k2,
                                                               @NonNull V v2) {
        Objects.requireNonNull(k1);
        Objects.requireNonNull(v1);

        Objects.requireNonNull(k2);
        Objects.requireNonNull(v2);

        if (Objects.equals(k1, k2)) {
            throw new IllegalArgumentException("Maps.of duplicate keys");
        }

        return new MapImpls.Map2<>(k1, v1, k2, v2);
    }

    public static <K, V> @Unmodifiable MLImmutableMap<K, V> of(@NonNull K k1, @NonNull V v1, @NonNull K k2, @NonNull V v2, @NonNull K k3, @NonNull V v3) {
        Objects.requireNonNull(k1);
        Objects.requireNonNull(v1);
        Objects.requireNonNull(k2);
        Objects.requireNonNull(v2);
        Objects.requireNonNull(k3);
        Objects.requireNonNull(v3);

        if (Objects.equals(k1, k2) || Objects.equals(k1, k3)) {
            throw new IllegalArgumentException("Maps.of duplicate keys");
        }

        return new MapImpls.Map3<>(k1, v1, k2, v2, k3, v3);
    }

    public static <K, V> @Unmodifiable MLImmutableMap<K, V> of(@NonNull K k1, @NonNull V v1, @NonNull K k2, @NonNull V v2, @NonNull K k3, @NonNull V v3, @NonNull K k4, @NonNull V v4) {
        return new MapImpls.MapN<>(
                Sets.of(
                        entry(k1, v1),
                        entry(k2, v2),
                        entry(k3, v3),
                        entry(k4, v4)
                )
        );
    }

    public static <K, V> @Unmodifiable MLImmutableMap<K, V> of(@NonNull K k1, @NonNull V v1, @NonNull K k2, @NonNull V v2, @NonNull K k3, @NonNull V v3, @NonNull K k4, @NonNull V v4, @NonNull K k5, @NonNull V v5) {
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

    @NonNull
    public static <K, V> @Unmodifiable MLImmutableMap<K, V> of(@NonNull K k1, @NonNull V v1, @NonNull K k2, @NonNull V v2, @NonNull K k3, @NonNull V v3, @NonNull K k4, @NonNull V v4, @NonNull K k5, @NonNull V v5, @NonNull K k6, @NonNull V v6) {
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


    @NonNull
    public static <K, V> @Unmodifiable MLImmutableMap<K, V> of(@NonNull K k1, @NonNull V v1, @NonNull K k2, @NonNull V v2, @NonNull K k3, @NonNull V v3, @NonNull K k4, @NonNull V v4, @NonNull K k5, @NonNull V v5, @NonNull K k6, @NonNull V v6, @NonNull K k7, @NonNull V v7) {
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

    @NonNull
    public static <K, V> @Unmodifiable MLImmutableMap<K, V> of(@NonNull K k1, @NonNull V v1, @NonNull K k2, @NonNull V v2, @NonNull K k3, @NonNull V v3, @NonNull K k4, @NonNull V v4, @NonNull K k5, @NonNull V v5, @NonNull K k6, @NonNull V v6, @NonNull K k7, @NonNull V v7, @NonNull K k8, @NonNull V v8) {
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

    @NonNull
    public static <K, V> @Unmodifiable MLImmutableMap<K, V> of(@NonNull K k1, @NonNull V v1, @NonNull K k2, @NonNull V v2, @NonNull K k3, @NonNull V v3, @NonNull K k4, @NonNull V v4, @NonNull K k5, @NonNull V v5, @NonNull K k6, @NonNull V v6, @NonNull K k7, @NonNull V v7, @NonNull K k8, @NonNull V v8, @NonNull K k9, @NonNull V v9) {
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

    @NonNull
    public static <K, V> @Unmodifiable MLImmutableMap<K, V> of(@NonNull K k1, @NonNull V v1, @NonNull K k2, @NonNull V v2, @NonNull K k3, @NonNull V v3, @NonNull K k4, @NonNull V v4, @NonNull K k5, @NonNull V v5, @NonNull K k6, @NonNull V v6, @NonNull K k7, @NonNull V v7, @NonNull K k8, @NonNull V v8, @NonNull K k9, @NonNull V v9, @NonNull K k10, @NonNull V v10) {
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
    public static @NonNull <K, V> MLImmutableMap<K, V> of(@NonNull Map<K, V> map, @NonNull K k, @NonNull V v) {
        if (map instanceof MLImmutableMap) {
            return ((MLImmutableMap<K, V>) map).plus(k, v);
        }
        return new MapImpls.MapN<>(Sets.ofAll(map.entrySet()).plus(entry(k, v)));
    }

    @SafeVarargs
    @NonNull
    public static <K, V> @Unmodifiable MLImmutableMap<K, V> of(Map.@NonNull Entry<K, V> @NonNull ... entries) {
        if (entries.length == 0) {
            return empty();
        }
        if (entries.length == 1) {
            return of(entries[0].getKey(), entries[0].getValue());
        }

        return new MapImpls.MapN<>(Sets.of(entries));
    }

    public static <K, V> @Unmodifiable @NonNull MLImmutableMap<K, V> of(Collection<Map.Entry<K, V>> entries) {
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

    public static <K, V> Map.@NonNull @Unmodifiable Entry<K, V> entry(@NonNull K key, @NonNull V value) {
        return new MapImpls.MLEntry<>(key, value);
    }
}
