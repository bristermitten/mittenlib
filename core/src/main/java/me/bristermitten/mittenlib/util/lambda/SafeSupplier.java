package me.bristermitten.mittenlib.util.lambda;

import me.bristermitten.mittenlib.util.Errors;
import me.bristermitten.mittenlib.util.Result;

import java.util.function.Supplier;

import static me.bristermitten.mittenlib.util.Result.runCatching;

/**
 * A {@link Supplier} that can throw a checked  exception.
 *
 * @param <T> the type of the value supplied
 */
@FunctionalInterface
public interface SafeSupplier<T> {
    /**
     * Gets a result, possibly throwing an exception.
     *
     * @return the result
     */
    T get() throws Exception;

    /**
     * Get the result, catching any exceptions and wrapping them in a {@link Result}
     *
     * @return the result
     */

    default Result<T> getCatching() {
        return runCatching(this);
    }

    /**
     * Turn this {@link SafeSupplier} into a {@link Supplier} that sneaky throws any exceptions.
     *
     * @return a {@link Supplier} that sneaky throws any exceptions
     */
    default Supplier<T> asSupplier() {
        return () -> {
            try {
                return get();
            } catch (Exception e) {
                Errors.sneakyThrow(e);
                return null;
            }
        };
    }
}
