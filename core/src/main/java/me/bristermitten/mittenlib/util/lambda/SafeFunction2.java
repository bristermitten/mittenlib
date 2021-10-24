package me.bristermitten.mittenlib.util.lambda;

import me.bristermitten.mittenlib.util.Errors;
import me.bristermitten.mittenlib.util.Result;

import java.util.function.BiFunction;

import static me.bristermitten.mittenlib.util.Result.runCatching;

@FunctionalInterface
public interface SafeFunction2<T, R1, R2> {
    R2 apply(T t, R1 r1) throws Throwable;

    default SafeFunction<R1, R2> curry(T t) {
        return r1 -> apply(t, r1);
    }

    default Result<R2> applyCatching(T t, R1 r1) {
        return runCatching(() -> apply(t, r1));
    }

    default BiFunction<T, R1, R2> asBiFunction() {
        return (t, r1) -> {
            try {
                return apply(t, r1);
            } catch (Throwable e) {
                Errors.sneakyThrow(e);
                return null;
            }
        };
    }
}
