package me.bristermitten.mittenlib.util.lambda;

import me.bristermitten.mittenlib.util.Errors;
import me.bristermitten.mittenlib.util.Result;
import me.bristermitten.mittenlib.util.Unit;

/**
 * A {@link Runnable} that can throw a checked exception.
 */
@FunctionalInterface
public interface SafeRunnable {
    /**
     * Run the code, possibly throwing an exception.
     */
    void run() throws Exception;

    /**
     * Run the code, catching any exceptions and wrapping them in a {@link Result}
     *
     * @return the result
     */
    default Result<Unit> runCatching() {
        return Result.runCatching(() -> {
            run();
            return Unit.UNIT;
        });
    }

    /**
     * Turn this {@link SafeRunnable} into a {@link Runnable} that sneaky throws any exceptions.
     *
     * @return a {@link Runnable} that sneaky throws any exceptions
     */
    default Runnable asRunnable() {
        return () -> {
            try {
                run();
            } catch (Exception e) {
                Errors.sneakyThrow(e);
            }
        };
    }
}
