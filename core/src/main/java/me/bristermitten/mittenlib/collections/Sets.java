package me.bristermitten.mittenlib.collections;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.InlineMe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Utility functions for creating immutable sets
 */
public class Sets {
    private Sets() {
    }

    public static <E> @Unmodifiable MLImmutableSet<E> of() {
        return new SetImpls.Set0<>();
    }

    public static <E> @Unmodifiable MLImmutableSet<E> of(E e) {
        return new SetImpls.Set1<>(e);
    }

    public static <E> @Unmodifiable MLImmutableSet<E> of(E e1, E e2) {
        if (Objects.equals(e1, e2)) {
            return new SetImpls.Set1<>(e1); // unique elements
        }
        return new SetImpls.Set2<>(e1, e2);
    }

    public static <E> @Unmodifiable MLImmutableSet<E> of(E e1, E e2, E e3) {
        // unique elements checks
        if (Objects.equals(e1, e2) && Objects.equals(e1, e3)) {
            return new SetImpls.Set1<>(e1);
        }
        if (Objects.equals(e1, e2)) {
            return new SetImpls.Set2<>(e1, e3);
        }
        if (Objects.equals(e1, e3)) {
            return new SetImpls.Set2<>(e1, e2);
        }
        if (Objects.equals(e2, e3)) {
            return new SetImpls.Set2<>(e2, e1);
        }
        // all unique
        return new SetImpls.Set3<>(e1, e2, e3);
    }

    @SafeVarargs
    public static <E> @Unmodifiable MLImmutableSet<E> of(E... es) {
        if (es.length == 0) {
            return of();
        }
        if (es.length == 1) {
            return of(es[0]);
        }
        // unique elements checks
        Set<E> set = new HashSet<>(es.length);

        for (E e : es) {
            if (e == null) {
                throw new NullPointerException("Set cannot contain null elements");
            }
            set.add(e);
        }

        return new SetImpls.SetN<>(set);
    }

    /**
     * Create an immutable set from a collection of elements.
     * This method is not guaranteed to copy the collection, but is guaranteed to be immutable and unmodifiable, including if the provided collection changes.
     *
     * @param collection the collection to create the set from
     * @param <E>        the type of the elements in the collection
     * @return a new immutable set containing the elements of the collection
     */
    public static <E> @Unmodifiable MLImmutableSet<E> ofAll(Collection<E> collection) {
        if (collection.isEmpty()) {
            return of();
        }
        if (collection instanceof MLImmutableSet) {
            return (MLImmutableSet<E>) collection;
        }
        return new SetImpls.SetN<>(new HashSet<>(collection));
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


        return new SetImpls.UnionOf<>(a, difference(b, a)); // TODO: make more efficient wrt nested unions
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
        int expectedSize = Math.abs(a.size() - b.size());
        //noinspection UnstableApiUsage
        ImmutableSet.Builder<E> objectBuilder = ImmutableSet.builderWithExpectedSize(expectedSize);
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
    @Deprecated
    @InlineMe(replacement = "Sets.union(start, others)", imports = "me.bristermitten.mittenlib.collections.Sets")
    public static <E> @Unmodifiable Set<E> concat(@NotNull @Unmodifiable Set<E> start, @NotNull @Unmodifiable Set<E> others) {
        return union(start, others);
    }
}
