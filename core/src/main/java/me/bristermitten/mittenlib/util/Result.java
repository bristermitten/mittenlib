package me.bristermitten.mittenlib.util;

import me.bristermitten.mittenlib.util.lambda.SafeRunnable;
import me.bristermitten.mittenlib.util.lambda.SafeSupplier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public interface Result<T> {
    static <T> Result<T> ok(T t) {
        return new Ok<>(t);
    }

    static <T, E extends Exception> Result<T> fail(E e) {
        return new Fail<>(e);
    }

    /**
     * Safely runs a {@link SafeSupplier}, encapsulating the returned value or any thrown exception in a {@link Result}
     *
     * @param supplier the supplier to run
     * @param <T>      the type of the returned value
     * @return a {@link Result} containing the returned value or any thrown exception
     */
    static <T> Result<T> runCatching(SafeSupplier<T> supplier) {
        try {
            return ok(supplier.get());
        } catch (Exception e) {
            return fail(e);
        }
    }

    /**
     * Safely executes a {@link SafeRunnable}, ignoring the result and wrapping any thrown exception in a {@link Result}
     *
     * @param runnable The {@link SafeRunnable} to execute
     * @return A {@link Result} wrapping the exception, if any
     */
    static Result<Unit> execCatching(SafeRunnable runnable) {
        try {
            runnable.run();
            return Unit.unitResult();
        } catch (Exception e) {
            return fail(e);
        }
    }

    /**
     * Safely executes a {@link SafeSupplier} that returns another {@link Result},
     * wrapping the returned {@link Result}'s value or any thrown exception in a {@link Result}
     * <p>
     * This is roughly equivalent to doing {@code runCatching(supplier).flatMap(Function.identity())}
     *
     * @param supplier The {@link SafeSupplier} to execute
     * @param <T>      The type of the returned {@link Result}'s value
     * @return A {@link Result} wrapping the returned {@link Result}'s value or any thrown exception
     */

    static <T> Result<T> computeCatching(SafeSupplier<Result<T>> supplier) {
        try {
            Result<T> tResult = supplier.get();
            if (tResult instanceof Fail) {
                // Checking this is probably faster than potentially causing an exception with getOrThrow
                return tResult;
            } else {
                return ok(tResult.getOrThrow());
            }
        } catch (Exception e) {
            return fail(e);
        }
    }


    /**
     * Turns a collection of {@link Result}s into a {@link Result} of a collection of the same type
     * If any of the results are {@link Fail}s, the whole result will be {@link Fail}
     * Otherwise, the result will be {@link Ok} with the collection of values
     * The returned collection is not guaranteed to be the same type as the input collection
     *
     * @param results The collection of {@link Result}s
     * @param <T>     The type of the {@link Result}'s value
     * @return A {@link Result} holding all the values from the input collection or an exception if any of the results are {@link Fail}s
     * @see Futures#sequence(Collection) for the same functionality but with {@link java.util.concurrent.CompletableFuture}s
     */
    static <T> Result<Collection<T>> sequence(Collection<Result<T>> results) {
        if (results.isEmpty()) {
            return ok(Collections.emptySet());
        }
        Result<Collection<T>> accumulator = ok(new ArrayList<>());
        for (Result<T> result : results) {
            accumulator = accumulator.flatMap(collection -> result.map(t -> {
                collection.add(t);
                return collection;
            }));
        }
        return accumulator;
    }

    Optional<T> toOptional();

    Optional<Exception> error();

    default <R> Result<R> map(Function<T, R> function) {
        return flatMap(t -> ok(function.apply(t)));
    }

    Result<T> orElse(Supplier<Result<T>> supplier);

    <R> Result<R> flatMap(Function<T, Result<R>> function); // monad :D

    T getOrThrow();

    boolean isSuccess();

    boolean isFailure();


    class Fail<T, E extends Exception> implements Result<T> {
        private final E exception;

        public Fail(E exception) {
            this.exception = exception;
        }

        @Override
        public Optional<T> toOptional() {
            return Optional.empty();
        }

        @Override
        public Optional<Exception> error() {
            return Optional.of(exception);
        }

        @Override
        public Result<T> orElse(Supplier<Result<T>> supplier) {
            return supplier.get();
        }

        @Override
        public <R> Result<R> flatMap(Function<T, Result<R>> function) {
            //noinspection unchecked
            return (Result<R>) this;
        }

        @Override
        public T getOrThrow() {
            Errors.sneakyThrow(exception);
            return null;
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public boolean isFailure() {
            return true;
        }
    }

    class Ok<T> implements Result<T> {
        private final T value;

        public Ok(T value) {
            this.value = value;
        }

        @Override
        public Optional<T> toOptional() {
            return Optional.of(value);
        }

        @Override
        public Optional<Exception> error() {
            return Optional.empty();
        }

        @Override
        public Result<T> orElse(Supplier<Result<T>> supplier) {
            return this;
        }

        @Override
        public <R> Result<R> flatMap(Function<T, Result<R>> function) {
            return function.apply(value);
        }

        @Override
        public T getOrThrow() {
            return value;
        }

        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public boolean isFailure() {
            return false;
        }
    }
}
