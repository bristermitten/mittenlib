package me.bristermitten.mittenlib.collections;

import org.jetbrains.annotations.NotNull;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.function.Predicate;

public abstract class MLImmutableSet<E> extends AbstractSet<E> {
    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("Immutable set");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Immutable set");
    }

    @Override
    public boolean add(E e) {
        throw new UnsupportedOperationException("Immutable set");
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends E> c) {
        throw new UnsupportedOperationException("Immutable set");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("Immutable set");
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException("Immutable set");
    }

    @Override
    public boolean removeIf(@NotNull Predicate<? super E> filter) {
        throw new UnsupportedOperationException("Immutable set");
    }


    public abstract MLImmutableSet<E> plus(E e);
}
