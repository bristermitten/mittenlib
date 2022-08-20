package me.bristermitten.mittenlib.util.lambda;

import me.bristermitten.mittenlib.util.Errors;

import java.util.function.BiConsumer;

@FunctionalInterface
public interface SafeBiConsumer<T, T2> {
    void consume(T t, T2 t2) throws Exception;

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
