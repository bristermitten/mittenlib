package me.bristermitten.mittenlib.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Futures {
    private Futures() {
    }

    /**
     * Transform an array of {@link CompletableFuture}s into a single {@link CompletableFuture} holding a <code>Collection&lt;T&gt;</code>
     * This is similar to Haskell's sequence / sequenceA functions, but specialised for CompletableFutures
     *
     * @param futures An array of futures
     * @param <T>     The type of the future
     * @return A single future that will be completed when all of the arguments are, containing all of the argument results in a List
     */
    @SafeVarargs
    public static <T> CompletableFuture<Collection<T>> sequence(CompletableFuture<T>... futures) {
        return CompletableFuture.allOf(futures)
                .thenApply(v -> {
                    List<T> list = new ArrayList<>(futures.length);
                    for (CompletableFuture<T> future : futures) {
                        T join = future.join();
                        list.add(join);
                    }
                    return list;
                });
    }

    /**
     * Transform a collection of {@link CompletableFuture}s into a single {@link CompletableFuture} holding a <code>Collection&lt;T&gt;</code>
     *
     * @param futures An array of futures
     * @param <T>     The type of the future
     * @return A single future that will be completed when all of the arguments are, containing all of the argument results in a List
     * @see Futures#sequence(CompletableFuture[])
     */
    public static <T> CompletableFuture<Collection<T>> sequence(Collection<CompletableFuture<T>> futures) {
        //noinspection unchecked
        return sequence(futures.toArray(new CompletableFuture[0]));
    }
}
