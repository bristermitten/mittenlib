package me.bristermitten.mittenlib.util;

import com.google.common.collect.Iterators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * Utility class for working with {@link Future}s and {@link CompletableFuture}s
 */
public class Futures {
    private Futures() {
    }

    /**
     * Transform an array of {@link CompletableFuture}s into a single {@link CompletableFuture} holding a <code>Collection&lt;T&gt;</code>
     * This is similar to (<a href="https://hackage.haskell.org/package/base-4.16.2.0/docs/GHC-Base.html#v:sequence">Haskell's sequence function</a>),
     * but specialised for CompletableFutures because we don't have higher kinded types in Java :(
     *
     * @param futures An array of futures
     * @param <T>     The type of the future
     * @return A single future that will be completed when all the arguments have completed, containing all the argument results in a List.
     * The order of the results should be the same as the order of the arguments, but this is not guaranteed.
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
     * @param futures A collection of futures
     * @param <T>     The type of the future
     * @return A single future that will be completed when all the arguments have completed, containing all the argument results in a List.
     * @see Futures#sequence(CompletableFuture[])
     */
    public static <T> CompletableFuture<Collection<T>> sequence(Collection<CompletableFuture<T>> futures) {
        //noinspection unchecked
        return sequence(futures.toArray(new CompletableFuture[0]));
    }

    /**
     * Transform an {@link Iterable} of {@link CompletableFuture}s into a single {@link CompletableFuture} holding a <code>Collection&lt;T&gt;</code>
     *
     * @param futures An iterable of futures
     * @param <T>     The type of the futures
     * @return A single future that will be completed when all the arguments have completed, containing all the argument results in a List.
     * @see Futures#sequence(CompletableFuture[])
     */
    public static <T> CompletableFuture<Collection<T>> sequence(Iterable<CompletableFuture<T>> futures) {
        List<CompletableFuture<T>> futuresList = new LinkedList<>();
        Iterators.addAll(futuresList, futures.iterator());
        return sequence(futuresList);
    }
}
