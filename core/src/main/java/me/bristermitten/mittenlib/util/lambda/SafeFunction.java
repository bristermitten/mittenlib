package me.bristermitten.mittenlib.util.lambda;

import me.bristermitten.mittenlib.util.Errors;
import me.bristermitten.mittenlib.util.Result;

import java.util.function.Function;

import static me.bristermitten.mittenlib.util.Result.runCatching;

/**
 * A {@link Function} that can throw a checked exception.
 *
 * @param <T> the type of the input
 * @param <R> the type of the result
 */
@FunctionalInterface
public interface SafeFunction<T, R> {
    /**
     * A {@link SafeFunction} that always returns the same value, ignoring the input.
     *
     * @param r   the value to return
     * @param <T> the type of the input
     * @param <R> the type of the result
     * @return a {@link SafeFunction} that always returns the given value
     */
    static <T, R> SafeFunction<T, R> constant(R r) {
        return unused -> r;
    }

    /**
     * Apply the function, possibly throwing an exception.
     *
     * @param t the input
     * @return the result
     */
    R apply(T t) throws Exception;

    /**
     * Apply the function, catching any exceptions and wrapping them in a {@link Result}
     *
     * @param t the input
     * @return the result
     */

    default Result<R> applyCatching(T t) {
        return runCatching(() -> apply(t));
    }

    /**
     * Turn this {@link SafeFunction} into a {@link Function} that sneaky throws any exceptions.
     *
     * @return a {@link Function} that sneaky throws any exceptions
     */
    default Function<T, R> asFunction() {
        return t -> {
            try {
                return apply(t);
            } catch (Exception e) {
                Errors.sneakyThrow(e);
                return null;
            }
        };
    }
}
