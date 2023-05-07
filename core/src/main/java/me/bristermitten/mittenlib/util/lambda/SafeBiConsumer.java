package me.bristermitten.mittenlib.util.lambda;

import me.bristermitten.mittenlib.util.Errors;

import java.util.function.BiConsumer;

/**
 * A {@link BiConsumer} that can throw a checked exception.
 *
 * @param <T>  the first type of the input
 * @param <T2> the second type of the input
 */
@FunctionalInterface
public interface SafeBiConsumer<T, T2> {
    /**
     * Consume the input, possibly throwing an exception.
     *
     * @param t  the first input
     * @param t2 the second input
     */
    void consume(T t, T2 t2) throws Exception;

    /**
     * Turn this {@link SafeBiConsumer} into a {@link BiConsumer} that sneaky throws any exceptions.
     *
     * @return a {@link BiConsumer} that sneaky throws any exceptions
     */

    default BiConsumer<T, T2> asBiConsumer() {
        return (t, t2) -> {
            try {
                consume(t, t2);
            } catch (Exception e) {
                Errors.sneakyThrow(e);
            }
        };
    }
}
