package me.bristermitten.mittenlib.collections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.AbstractMap;
import java.util.function.BiFunction;
import java.util.function.Function;

@Unmodifiable
public abstract class MLImmutableMap<K, V> extends AbstractMap<K, V> {
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

    public abstract MLImmutableMap<K, V> plus(@NotNull K key, @NotNull V value);
}
