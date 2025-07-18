package me.bristermitten.mittenlib.collections;

import org.jetbrains.annotations.NotNull;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Implementations for immutable Maps used in {@link Maps}
 */
public class MapImpls {
    private MapImpls() {
    }

    abstract static class MLMap<K, V> extends AbstractMap<K, V> {
        @Override
        public boolean remove(Object key, Object value) {
            throw new UnsupportedOperationException("Immutable map");
        }

        @Override
        public V put(K key, V value) {
            throw new UnsupportedOperationException("Immutable map");
        }

        @Override
        public V putIfAbsent(K key, V value) {
            throw new UnsupportedOperationException("Immutable map");
        }

        @Override
        public V replace(K key, V value) {
            throw new UnsupportedOperationException("Immutable map");
        }

        @Override
        public boolean replace(K key, V oldValue, V newValue) {
            throw new UnsupportedOperationException("Immutable map");
        }

        @Override
        public V computeIfAbsent(K key, @NotNull Function<? super K, ? extends V> mappingFunction) {
            throw new UnsupportedOperationException("Immutable map");
        }

        @Override
        public V computeIfPresent(K key, @NotNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
            throw new UnsupportedOperationException("Immutable map");
        }

        @Override
        public V compute(K key, @NotNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
            throw new UnsupportedOperationException("Immutable map");
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException("Immutable map");
        }

        @Override
        public V remove(Object key) {
            throw new UnsupportedOperationException("Immutable map");
        }
    }

    static class Map1<K, V> extends MLMap<K, V> {
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

    static class Map2<K, V> extends MLMap<K, V> {
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
            this.entrySet = Sets.of(new SimpleEntry<>(k1, v1), new SimpleEntry<>(k2, v2));
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
        public int hashCode() {
            return Objects.hash(k1, v1, k2, k2);
        }
    }

    static class Map3<K, V> extends MLMap<K, V> {
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
            this.entrySet = Sets.of(new SimpleEntry<>(k1, v1),
                    new SimpleEntry<>(k2, v2),
                    new SimpleEntry<>(k3, v3));
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
        public int hashCode() {
            return Objects.hash(k1, v1, k2, v2, k3, v3);
        }
    }

    static class MapN<K, V> extends AbstractMap<K, V> {
        private final @NotNull Set<Entry<K, V>> entrySet;

        MapN(@NotNull Set<Entry<K, V>> entrySet) {
            this.entrySet = entrySet;
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
            return Objects.hash(entrySet);
        }
    }
}
