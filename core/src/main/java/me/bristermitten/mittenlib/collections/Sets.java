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
}
