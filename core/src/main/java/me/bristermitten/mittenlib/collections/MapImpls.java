package me.bristermitten.mittenlib.collections;

import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Implementations for immutable Maps used in {@link Maps}
 */
public class MapImpls {
    private MapImpls() {
    }

    static class MLEntry<K, V> implements Map.Entry<K, V> {
        private final K key;
        private final V value;

        MLEntry(@NotNull K key, @NotNull V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            throw new UnsupportedOperationException("Immutable entry");
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Map.Entry)) return false;
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) obj;
            return Objects.equals(key, entry.getKey()) && Objects.equals(value, entry.getValue());
        }

        @Override
        public int hashCode() {
            return key.hashCode() ^ value.hashCode();
        }
    }

    static class Map0<K, V> extends MLImmutableMap<K, V> { //NOSONAR
        @Override
        public boolean containsKey(Object key) {
            return false;
        }

        @Override
        public boolean containsValue(Object value) {
            return false;
        }

        @NotNull
        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            return Collections.emptySet();
        }

        @Override
        public MLImmutableMap<K, V> plus(@NotNull K key, @NotNull V value) {
            return Maps.of(key, value);
        }
    }

    static class Map1<K, V> extends MLImmutableMap<K, V> {
        private final @NotNull K k;
        private final @NotNull V v;
        private final @NotNull Set<Entry<K, V>> entrySet;

        Map1(@NotNull K k, @NotNull V v) {
            this.k = k;
            this.v = v;
            this.entrySet = Sets.of(new MLEntry<>(k, v));
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
        public MLImmutableMap<K, V> plus(@NotNull K key, @NotNull V value) {
            return new Map2<>(k, v, key, value);
        }
    }

    static class Map2<K, V> extends MLImmutableMap<K, V> {
        private final @NotNull K k1;
        private final @NotNull K k2;
        private final @NotNull V v1;
        private final @NotNull V v2;
        private final @NotNull Set<Entry<K, V>> entrySet;

        Map2(@NotNull K k1, @NotNull V v1, @NotNull K k2, @NotNull V v2) {
            this.k1 = k1;
            this.k2 = k2;
            this.v1 = v1;
            this.v2 = v2;
            this.entrySet = Sets.of(new MLEntry<>(k1, v1), new MLEntry<>(k2, v2));
        }


        @Override
        public boolean containsKey(Object key) {
            return k1.equals(key) || k2.equals(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return v1.equals(value) || v2.equals(value);
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
        public MLImmutableMap<K, V> plus(@NotNull K key, @NotNull V value) {
            return new Map3<>(k1, v1, k2, v2, key, value);
        }
    }

    static class Map3<K, V> extends MLImmutableMap<K, V> {
        private final @NotNull K k1;
        private final @NotNull K k2;
        private final @NotNull K k3;
        private final @NotNull V v1;
        private final @NotNull V v2;
        private final @NotNull V v3;
        private final @NotNull Set<Entry<K, V>> entrySet;

        Map3(@NotNull K k1, @NotNull V v1, @NotNull K k2, @NotNull V v2, @NotNull K k3, @NotNull V v3) {
            this.k1 = k1;
            this.k2 = k2;
            this.k3 = k3;
            this.v1 = v1;
            this.v2 = v2;
            this.v3 = v3;
            this.entrySet = Sets.of(new MLEntry<>(k1, v1),
                    new MLEntry<>(k2, v2),
                    new MLEntry<>(k3, v3));
        }

        @Override
        public boolean containsKey(Object key) {
            return k1.equals(key) || k2.equals(key) || k3.equals(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return v1.equals(value) || v2.equals(value) || v3.equals(value);
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
        public MLImmutableMap<K, V> plus(@NotNull K key, @NotNull V value) {
            return new MapN<>(
                    Sets.of(
                            new MLEntry<>(k1, v1),
                            new MLEntry<>(k2, v2),
                            new MLEntry<>(k3, v3),
                            new MLEntry<>(key, value)
                    )
            );
        }
    }

    static class MapN<K, V> extends MLImmutableMap<K, V> {
        private final @NotNull MLImmutableSet<Entry<K, V>> entrySet;

        MapN(@NotNull MLImmutableSet<Entry<K, V>> entrySet) {
            this.entrySet = entrySet;
        }


        @NotNull
        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            return entrySet;
        }


        @Override
        public MLImmutableMap<K, V> plus(@NotNull K key, @NotNull V value) {
            Set<Entry<K, V>> newEntries = new HashSet<>(entrySet);
            newEntries.add(new MLEntry<>(key, value));
            return new MapN<>(Sets.ofAll(newEntries));
        }
    }
}
