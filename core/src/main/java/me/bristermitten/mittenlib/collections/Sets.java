package me.bristermitten.mittenlib.collections;

import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Utility functions for creating immutable sets
 */
public class Sets {
    private Sets() {
    }

    public static <E> @Unmodifiable Set<E> of() {
        return Collections.emptySet();
    }

    public static <E> @Unmodifiable Set<E> of(E e) {
        return new SetImpls.Set1<>(e);
    }

    public static <E> @Unmodifiable Set<E> of(E e1, E e2) {
        if (Objects.equals(e1, e2)) {
            return new SetImpls.Set1<>(e1); // unique elements
        }
        return new SetImpls.Set2<>(e1, e2);
    }

    public static <E> @Unmodifiable Set<E> ofAll(Collection<E> collection) {
        if (collection.isEmpty()) {
            return of();
        }
        if (collection instanceof SetImpls.MLImmutableSet) {
            return (Set<E>) collection;
        }
        return ImmutableSet.copyOf(collection);
    }

    /**
     * Returns a new set containing the elements of 2 given sets
     * The returned set is immutable. The passed sets should be also be immutable.
     * <b>Changes to the underlying sets are not guaranteed to be reflected!</b>
     *
     * @param a   the first set
     * @param b   the other set
     * @param <E> the type of the elements
     * @return a new set containing the elements of the given sets
     */
    public static <E> @Unmodifiable Set<E> union(@NotNull @Unmodifiable Set<E> a, @NotNull @Unmodifiable Set<E> b) {
        if (a.isEmpty() && b.isEmpty()) {
            return of();
        }
        if (a.isEmpty()) {
            return b;
        }
        if (b.isEmpty()) {
            return a;
        }
        return new SetImpls.UnionOf<>(a, b); // TODO: make more efficient wrt nested unions
    }

    /**
     * Return a new set containing the difference of 2 sets, i.e. all the elements in {@code a} that are not in {@code b}
     *
     * @param a   the main set
     * @param b   the other set
     * @param <E> the type of the elements
     * @return the difference of {@code a} and {@code b}
     */
    public static <E> @Unmodifiable Set<E> difference(@NotNull @Unmodifiable Set<E> a, @NotNull @Unmodifiable Set<E> b) {
        if (a.isEmpty() && b.isEmpty()) {
            return of(); // {} \\ {} = {}
        }
        if (a.isEmpty()) {
            return of(); // {} \\ a = {}
        }
        if (b.isEmpty()) {
            return a; // a \\ {} = a
        }
        //noinspection UnstableApiUsage
        ImmutableSet.Builder<E> objectBuilder = ImmutableSet.builderWithExpectedSize(a.size() - b.size());
        for (E e : a) {
            if (!b.contains(e)) {
                objectBuilder.add(e);
            }
        }
        return objectBuilder.build();
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
     * @deprecated Use {@link Sets#union(Set, Set)}
     */
    @Deprecated()
    public static <E> @Unmodifiable Set<E> concat(@NotNull @Unmodifiable Set<E> start, @NotNull @Unmodifiable Set<E> others) {
        return union(start, others);
    }
}
