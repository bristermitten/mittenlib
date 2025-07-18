package me.bristermitten.mittenlib.util.lambda;

import me.bristermitten.mittenlib.util.Errors;
import me.bristermitten.mittenlib.util.Result;

import java.io.IOException;
import java.util.function.Function;

import static me.bristermitten.mittenlib.util.Result.runCatching;

/**
 * Like {@link SafeFunction} but only for {@link IOException}
 *
 * @param <T> the type of the input
 * @param <R> the type of the result
 */
@FunctionalInterface
public interface IOFunction<T, R> {
    /**
     * A {@link IOFunction} that always returns the same value, ignoring the input.
     *
     * @param r   the value to return
     * @param <T> the type of the input
     * @param <R> the type of the result
     * @return a {@link IOFunction} that always returns the given value
     */
    static <T, R> IOFunction<T, R> constant(R r) {
        return unused -> r;
    }

    /**
     * Wrap a {@link Function} in a {@link IOFunction}
     *
     * @param function the function to wrap
     * @param <T>      the type of the input
     * @param <R>      the type of the result
     * @return a {@link IOFunction} that delegates to the given function
     */
    static <T, R> IOFunction<T, R> of(Function<T, R> function) {
        return function::apply;
    }

    /**
     * Apply the function, possibly throwing an exception.
     *
     * @param t the input
     * @return the result
     * @throws IOException if an I/O error occurs
     */
    R apply(T t) throws IOException;

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
     * Turn this {@link IOFunction} into a {@link Function} that sneaky throws any exceptions.
     *
     * @return a {@link Function} that sneaky throws any exceptions
     */
    default Function<T, R> asFunction() {
        return t -> {
            try {
                return apply(t);
            } catch (IOException e) {
                Errors.sneakyThrow(e);
                return null;
            }
        };
    }
}
