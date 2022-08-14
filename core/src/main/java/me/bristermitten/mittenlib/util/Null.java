package me.bristermitten.mittenlib.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.function.Function;
import java.util.function.Supplier;

public class Null {
    private Null() {

    }

    @Contract("null, null -> null; _, _ -> !null")
    public static <T> T orElse(@Nullable T t, T other) {
        return t == null ? other : t;
    }


    public static <T> @NotNull T orElse(@Nullable T t, @NotNull Supplier<@NotNull T> other) {
        return t == null ? other.get() : t;
    }

    @Contract("null, _ -> null; !null, _ -> !null")
    public static <A, B> @Nullable B map(@Nullable A a, Function<A, @NotNull B> function) {
        if (a == null) {
            return null;
        }
        return function.apply(a);
    }
}
