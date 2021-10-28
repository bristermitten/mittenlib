package me.bristermitten.mittenlib.collections;

import java.util.Set;

/**
 * Utility functions for creating immutable sets
 * Unnecessary in Java 9+
 */
public class Sets {
    private Sets() {
    }

    public static <E> Set<E> of(E e) {
        return new SetImpls.Set1<>(e);
    }

    public static <E> Set<E> of(E e1, E e2) {
        return new SetImpls.Set2<>(e1, e2);
    }
}
