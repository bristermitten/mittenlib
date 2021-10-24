package me.bristermitten.mittenlib.util;

import java.util.Optional;
import java.util.function.Function;

public interface Result<T> {
    static <T> Result<T> ok(T t) {
        return new Ok<>(t);
    }

    static <T, E extends Throwable> Result<T> fail(E e) {
        return new Fail<>(e);
    }

    static <T> Result<T> runCatching(SafeSupplier<T> supplier) {
        try {
            return ok(supplier.get());
        } catch (Throwable e) {
            return fail(e);
        }
    }


    Optional<T> toOptional();

    Optional<Throwable> error();

    default <R> Result<R> map(Function<T, R> function) {
        return flatMap(t -> ok(function.apply(t)));
    }

    <R> Result<R> flatMap(Function<T, Result<R>> function); // monad :D

    T getOrThrow();

    class Fail<T, E extends Throwable> implements Result<T> {
        private final E exception;

        public Fail(E exception) {
            this.exception = exception;
        }

        @Override
        public Optional<T> toOptional() {
            return Optional.empty();
        }

        @Override
        public Optional<Throwable> error() {
            return Optional.of(exception);
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
        public Optional<Throwable> error() {
            return Optional.empty();
        }

        @Override
        public <R> Result<R> flatMap(Function<T, Result<R>> function) {
            return function.apply(value);
        }

        @Override
        public T getOrThrow() {
            return value;
        }
    }
}
