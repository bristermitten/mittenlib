package me.bristermitten.mittenlib.collections;

import org.jspecify.annotations.NonNull;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.function.Predicate;

public abstract class MLImmutableSet<E> extends AbstractSet<@NonNull E> {
    @Override
    public boolean remove(@NonNull Object o) {
        throw new UnsupportedOperationException("Immutable set");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Immutable set");
    }

    @Override
    public boolean add(@NonNull E e) {
        throw new UnsupportedOperationException("Immutable set");
    }

    @Override
    public boolean addAll(@NonNull Collection<? extends E> c) {
        throw new UnsupportedOperationException("Immutable set");
    }

    @Override
    public boolean removeAll(@NonNull Collection<?> c) {
        throw new UnsupportedOperationException("Immutable set");
    }

    @Override
    public boolean retainAll(@NonNull Collection<?> c) {
        throw new UnsupportedOperationException("Immutable set");
    }

    @Override
    public boolean removeIf(@NonNull Predicate<? super E> filter) {
        throw new UnsupportedOperationException("Immutable set");
    }


    public abstract @NonNull MLImmutableSet<E> plus(@NonNull E e);
}
