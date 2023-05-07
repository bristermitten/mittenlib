package me.bristermitten.mittenlib.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Utility class for working with nullable values without having to use {@link java.util.Optional}.
 */
public class Null {
    private Null() {

    }

    /**
     * Returns the given value if it is not null, or the given other value if it is.
     * Analogous to {@link java.util.Optional#orElse(Object)} or Kotlin's Elvis operator.
     *
     * @param t     the value to check
     * @param other the value to return if t is null
     * @param <T>   the type of the value
     * @return {@code t} if it is not null, or {@code other} if it is
     */
    @Contract("null, null -> null; _, _ -> !null")
    public static <T> T orElse(@Nullable T t, T other) {
        return t == null ? other : t;
    }


    /**
     * Returns the given value if it is not null, or the value returned by the given {@link Supplier} if it is.
     * Analogous to {@link java.util.Optional#orElseGet(Supplier)}
     *
     * @param t     the value to check
     * @param other the supplier to use if t is null
     * @param <T>   the type of the value
     * @return {@code t} if it is not null, or the value returned by {@code other} if it is
     */
    @Contract("_, _ -> !null")
    public static <T> @NotNull T orElse(@Nullable T t, @NotNull Supplier<@NotNull T> other) {
        return t == null ? other.get() : t;
    }

    /**
     * Applies the given function to the given value if it is not null, or returns null if it is.
     * Analogous to {@link java.util.Optional#map(Function)} or Kotlin's {@code ?.} operator.
     *
     * @param a        the nullable value to map
     * @param function the function to apply
     * @param <A>      the type of the nullable value
     * @param <B>      the type of the mapped value
     * @return the result of applying the function to the value, or null if the value is null
     */
    @Contract("null, _ -> null; !null, _ -> !null")
    public static <A, B> @Nullable B map(@Nullable A a, Function<A, @NotNull B> function) {
        if (a == null) {
            return null;
        }
        return function.apply(a);
    }
}
