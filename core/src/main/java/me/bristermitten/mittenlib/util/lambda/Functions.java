package me.bristermitten.mittenlib.util.lambda;

import java.util.function.Function;

/**
 * Utility class for dealing with {@link Function}s
 */
public class Functions {
    private Functions() {

    }

    /**
     * A function that always returns the same value, ignoring the input
     *
     * @param r   the value to return
     * @param <A> the input type
     * @param <R> the return type
     * @return a function that always returns the input value
     */
    public static <A, R> Function<A, R> constant(R r) {
        return unused -> r;
    }
}
