package me.bristermitten.mittenlib.util;

import java.util.function.Supplier;

public interface SafeSupplier<T> {
    T get() throws Throwable;

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
