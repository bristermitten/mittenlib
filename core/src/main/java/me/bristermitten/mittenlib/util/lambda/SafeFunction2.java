package me.bristermitten.mittenlib.util.lambda;

import me.bristermitten.mittenlib.util.Errors;
import me.bristermitten.mittenlib.util.Result;

import java.util.function.BiFunction;

import static me.bristermitten.mittenlib.util.Result.runCatching;

/**
 * A {@link BiFunction} that can throw a checked exception.
 *
 * @param <T>  the type of the first argument
 * @param <R1> the type of the second argument
 * @param <R2> the type of the result
 */
@FunctionalInterface
public interface SafeFunction2<T, R1, R2> {
    /**
     * Apply the function, possibly throwing an exception.
     *
     * @param t  the first argument
     * @param r1 the second argument
     * @return the result
     */
    R2 apply(T t, R1 r1) throws Exception;

    /**
     * Curry the first argument of this function, returning a {@link SafeFunction} that takes only the second argument.
     *
     * @param t the first argument
     * @return a curried {@link SafeFunction}
     */
    default SafeFunction<R1, R2> curry(T t) {
        return r1 -> apply(t, r1);
    }

    /**
     * Apply the function, catching any exceptions and wrapping them in a {@link Result}
     *
     * @param t  the first argument
     * @param r1 the second argument
     * @return the result
     */
    default Result<R2> applyCatching(T t, R1 r1) {
        return runCatching(() -> apply(t, r1));
    }

    /**
     * Turn this {@link SafeFunction2} into a {@link BiFunction} that sneaky throws any exceptions.
     *
     * @return a {@link BiFunction} that sneaky throws any exceptions
     */
    default BiFunction<T, R1, R2> asBiFunction() {
        return (t, r1) -> {
            try {
                return apply(t, r1);
            } catch (Exception e) {
                Errors.sneakyThrow(e);
                return null;
            }
        };
    }
}
