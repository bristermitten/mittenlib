package me.bristermitten.mittenlib.collections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.Set;

/**
 * Utility functions for creating immutable sets
 * Unnecessary in Java 9+
 */
public class Sets {
    private Sets() {
    }

    public static <E> @Unmodifiable Set<E> of(E e) {
        return new SetImpls.Set1<>(e);
    }

    public static <E> @Unmodifiable Set<E> of(E e1, E e2) {
        return new SetImpls.Set2<>(e1, e2);
    }

    /**
     * Returns a new set containing the elements of 2 given sets
     * The returned set is immutable. The passed sets should be also be immutable.
     * <b>Changes to the underlying sets are not guaranteed to be reflected!</b>
     *
     * @param start  the first set
     * @param others the other set
     * @param <E>    the type of the elements
     * @return a new set containing the elements of the given sets
     */
    public static <E> @Unmodifiable Set<E> concat(@NotNull @Unmodifiable Set<E> start, @NotNull @Unmodifiable Set<E> others) {
        if (start.isEmpty() && others.isEmpty()) {
            return Collections.emptySet();
        }
        if (start.isEmpty()) {
            return others;
        }
        if (others.isEmpty()) {
            return start;
        }
        return new SetImpls.ConcatenatingSet<>(start, others);
    }
}
