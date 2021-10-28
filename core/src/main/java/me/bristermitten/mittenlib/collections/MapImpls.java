package me.bristermitten.mittenlib.collections;

import org.jetbrains.annotations.NotNull;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Implementations for immutable Maps used in {@link Maps}
 */
public class MapImpls {
    private MapImpls() {
    }

    static class Map1<K, V> extends AbstractMap<K, V> {
        private final @NotNull K k;
        private final @NotNull V v;
        private final @NotNull Set<Entry<K, V>> entrySet;

        Map1(@NotNull K k, @NotNull V v) {
            this.k = k;
            this.v = v;
            this.entrySet = Sets.of(new SimpleEntry<>(k, v));
        }

        @Override
        public boolean containsKey(Object key) {
            return k.equals(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return v.equals(value);
        }

        @NotNull
        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            return entrySet;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Map)) return false;
            return this.entrySet().equals(((Map<?, ?>) o).entrySet());
        }

        @Override
        public int hashCode() {
            return Objects.hash(k, v);
        }
    }
}
