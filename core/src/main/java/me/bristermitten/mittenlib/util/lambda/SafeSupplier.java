package me.bristermitten.mittenlib.util.lambda;

import me.bristermitten.mittenlib.util.Errors;
import me.bristermitten.mittenlib.util.Result;

import java.util.function.Supplier;

import static me.bristermitten.mittenlib.util.Result.runCatching;

@FunctionalInterface
public interface SafeSupplier<T> {
    T get() throws Throwable;

    default Result<T> getCatching() {
        return runCatching(this);
    }

    default Supplier<T> asSupplier() {
        return () -> {
            try {
                return get();
            } catch (Throwable e) {
                Errors.sneakyThrow(e);
                return null;
            }
        };
    }
}
