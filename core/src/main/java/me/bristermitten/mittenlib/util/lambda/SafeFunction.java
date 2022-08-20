package me.bristermitten.mittenlib.util.lambda;

import me.bristermitten.mittenlib.util.Errors;
import me.bristermitten.mittenlib.util.Result;

import java.util.function.Function;

import static me.bristermitten.mittenlib.util.Result.runCatching;

@FunctionalInterface
public interface SafeFunction<T, R> {
    static <T, R> SafeFunction<T, R> constant(R r) {
        return unused -> r;
    }

    R apply(T t) throws Exception;

    default Result<R> applyCatching(T t) {
        return runCatching(() -> apply(t));
    }

    default Function<T, R> asFunction() {
        return t -> {
            try {
                return apply(t);
            } catch (Exception e) {
                Errors.sneakyThrow(e);
                return null;
            }
        };
    }
}
