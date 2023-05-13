package me.bristermitten.mittenlib.util;

import me.bristermitten.mittenlib.util.lambda.SafeConsumer;
import me.bristermitten.mittenlib.util.lambda.SafeFunction;
import me.bristermitten.mittenlib.util.lambda.SafeRunnable;
import me.bristermitten.mittenlib.util.lambda.SafeSupplier;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A {@link Result} encapsulates a computation that may fail, throwing an exception.
 * It either holds a value of type {@link T} or an exception.
 *
 * @param <T> the type of the value
 */
public interface Result<T> {
    /**
     * Create a {@link Result} from a given value. This will always be an {@link Ok}.
     *
     * @param t   the value
     * @param <T> the type of the value
     * @return a {@link Result} containing the given value
     */
    @Contract(pure = true, value = "_ -> new")
    static <T> @NotNull Result<T> ok(@NotNull T t) {
        return new Ok<>(t);
    }

    /**
     * Create a {@link Result} from a given exception. This will always be a {@link Fail}.
     *
     * @param e   the exception
     * @param <T> the type of the value
     * @param <E> the type of the exception
     * @return a {@link Result} containing the given exception
     */
    @Contract(pure = true, value = "_ -> new")
    static <T, E extends Exception> @NotNull Result<T> fail(@NotNull E e) {
        return new Fail<>(e);
    }

    /**
     * Safely runs a {@link SafeSupplier}, encapsulating the returned value or any thrown exception in a {@link Result}
     *
     * @param supplier the supplier to run
     * @param <T>      the type of the returned value
     * @return a {@link Result} containing the returned value or any thrown exception
     */
    @Contract(value = "_ -> new")
    static <T> @NotNull Result<T> runCatching(@NotNull SafeSupplier<T> supplier) {
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
    @Contract(value = "_ -> new")
    static @NotNull Result<Unit> execCatching(SafeRunnable runnable) {
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

    @Contract(value = "_ -> new")
    static <T> @NotNull Result<T> computeCatching(@NotNull SafeSupplier<Result<T>> supplier) {
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
     * Executes a function with a lazily generated resource, closing the resource afterwards, and handling any exceptions.
     * Unlike {@link #tryWithResources(AutoCloseable, SafeFunction)}, this method handles exceptions thrown in the resource supplier.
     *
     * @param resourceSupplier The supplier to generate the resource
     * @param function         The function to execute
     * @param <T>              The type of the returned {@link Result}'s value
     * @param <R>              The type of the resource
     * @return A {@link Result}, which is either the result of the function or an exception
     */
    static <T, R extends AutoCloseable> @NotNull Result<T> tryWithResources(@NotNull SafeSupplier<R> resourceSupplier, SafeFunction<R, Result<T>> function) {
        try (R resource = resourceSupplier.get()) {
            return tryWithResources(resource, function);
        } catch (Exception e) {
            return fail(e);
        }
    }

    /**
     * Executes a function with a resource, closing the resource afterwards, and handling any exceptions
     *
     * @param resource The resource to use
     * @param function The function to execute
     * @param <T>      The type of the returned {@link Result}'s value
     * @param <R>      The type of the resource
     * @return A {@link Result}, which is either the result of the function or an exception
     */
    static <T, R extends AutoCloseable> @NotNull Result<T> tryWithResources(@NotNull R resource, SafeFunction<R, Result<T>> function) {
        try (R r = resource) {
            return function.apply(r);
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
    @Contract(value = "_ -> new")
    static <T> @NotNull Result<Collection<T>> sequence(@NotNull Collection<@NotNull Result<T>> results) {
        if (results.isEmpty()) {
            return ok(Collections.emptySet());
        }

        // We could use a more functional approach here, but it would be less efficient
        List<T> collection = new ArrayList<>(results.size());
        for (Result<T> result : results) {
            if (result instanceof Fail) {
                //noinspection unchecked
                return (Result<Collection<T>>) result;
            } else if (result instanceof Ok) {
                collection.add(result.getOrThrow());
            }
        }
        return ok(collection);
    }

    /**
     * Turns the {@link Result} into an {@link Optional}.
     * If the {@link Result} is {@link Ok}, the {@link Optional} will contain the value.
     * If the {@link Result} is {@link Fail}, the {@link Optional} will be empty.
     *
     * @return The value, if present, in an {@link Optional}
     */

    @NotNull
    @Contract(pure = true)
    Optional<T> toOptional();

    /**
     * If this {@link Result} is {@link Fail}, returns the exception.
     * Otherwise, returns an empty {@link Optional}
     *
     * @return The exception, if present, in an {@link Optional}
     */
    @NotNull
    @Contract(pure = true)
    Optional<Exception> error();

    /**
     * Applies a given function to the {@link Result}, threading through the exception if the {@link Result} is {@link Fail}.
     * This would make {@link Result} a functor if it were not for the fact that the function can throw an exception.
     * <p>
     * <ul>
     * <li>If the {@link Result} is {@link Ok}, the function is applied to the value and the result is returned in a new {@link Ok}.</li>
     * <li>If the function throws an exception, the exception is returned in a new {@link Fail}.</li>
     * <li>If the {@link Result} is {@link Fail}, the exception is returned in a new {@link Fail}.</li>
     * </ul>
     *
     * @param function The function to apply
     * @param <R>      The type of the result of the function
     * @return A new {@link Result} containing the result of the function or the exception from this {@link Result}
     */
    @Contract(pure = true)
    @NotNull
    default <R> Result<R> map(SafeFunction<T, R> function) {
        return flatMap(t -> ok(function.apply(t)));
    }

    /**
     * Applies the given {@link SafeConsumer} if the {@link Result} is {@link Ok}.
     * If the {@link Result} is {@link Fail}, the result is returned unchanged.
     *
     * @param t The {@link SafeConsumer} to apply
     * @return The {@link Result} after the {@link SafeConsumer} has been applied
     */
    @Contract(pure = true)
    @NotNull
    default Result<Unit> ifOk(SafeConsumer<T> t) {
        return flatMap(t1 -> {
            t.consume(t1);
            return ok(Unit.UNIT);
        });
    }

    /**
     * Applies an "or" operation to the {@link Result}.
     * If either {@link Result} is {@link Ok}, {@link Ok} is returned, prioritizing the left {@link Result}.
     * Otherwise, the exception from the left {@link Result} is returned in a {@link Fail}
     *
     * @param supplier A supplier for the right {@link Result}
     * @return The result of the "or" operation
     */
    @Contract(pure = true)
    @NotNull Result<T> orElse(Supplier<Result<T>> supplier);

    /**
     * Applies a function to the {@link Result}, passing through the exception if the {@link Result} is {@link Fail}
     * This allows the composition of {@link Result}-ful functions, making {@link Result} a monad (technically not a lawful monad because the function can throw exceptions)
     * <p>
     * If the left {@link Result} is {@link Ok}, the function is applied to the value and the result is returned.
     * If the function throws an exception, it is caught and returned in a {@link Fail}
     * Otherwise, the exception from the left {@link Result} is returned in a {@link Fail}
     *
     * @param function The function to apply
     * @param <R>      The type of the result of the function
     * @return A new {@link Result} containing the result of the function or the exception from this {@link Result}
     * @see #flatMapPure(Function) for a version that cannot throw checked exceptions
     */
    @Contract(pure = true)
    @NotNull <R> Result<R> flatMap(SafeFunction<T, Result<R>> function);

    /**
     * Like {@link #flatMap(SafeFunction)}, but the function cannot throw checked exceptions
     *
     * @param function The function to apply
     * @param <R>      The type of the result of the function
     * @return A new {@link Result} containing the result of the function or the exception from this {@link Result}
     * @see #flatMap(SafeFunction) for a version that can throw checked exceptions
     */
    @Contract(pure = true)
    @NotNull
    default <R> Result<R> flatMapPure(Function<T, Result<R>> function) {
        return flatMap(function::apply);
    }

    /**
     * Replaces the value of the {@link Result} with a new value.
     * If the {@link Result} is {@link Ok}, the new value is returned in a new {@link Ok}.
     * Otherwise, the result is returned unchanged.
     *
     * @param r   The new value
     * @param <R> The type of the new value
     * @return A new {@link Result} containing the new value or the exception from this {@link Result}
     */
    default <R> Result<R> replace(R r) {
        return map(SafeFunction.constant(r));
    }

    /**
     * Replaces the value of the {@link Result} with the {@link Unit} value.
     * If the {@link Result} is {@link Ok}, {@link Unit#UNIT} is returned in a new {@link Ok}.
     * Otherwise, the result is returned unchanged.
     *
     * @return A new {@link Result} containing {@link Unit#UNIT} or the exception from this {@link Result}
     */
    default Result<Unit> void_() {
        return map(t -> Unit.UNIT);
    }


    /**
     * Returns the value if the {@link Result} is {@link Ok}, otherwise throws the exception
     *
     * @return The value
     * @throws RuntimeException The exception. Note that if the exception is a checked exception,
     *                          it will <b>not</b> be wrapped in a {@link RuntimeException}.
     *                          However, {@link RuntimeException} is used in the method signature to
     *                          avoid manual try/catch blocks
     */
    @NotNull T getOrThrow() throws RuntimeException;

    /**
     * Handle the 2 cases of the {@link Result} separately with 2 functions
     * If the {@link Result} is {@link Ok}, the first function is applied to the value.
     * If the {@link Result} is {@link Fail}, the second function is applied to the exception.
     *
     * @param successHandler   The function to apply if the {@link Result} is {@link Ok}
     * @param exceptionHandler The function to apply if the {@link Result} is {@link Fail}
     * @param <R>              The type of the result of the function
     * @return The result of the function applied to the value or exception
     */
    <R> R handle(Function<T, R> successHandler, Function<Exception, R> exceptionHandler);

    /**
     * Handles the exception case of the {@link Result} with a function.
     * If the {@link Result} is {@link Ok}, the value is returned.
     * If the {@link Result} is {@link Fail}, the function is applied to the exception.
     *
     * @param exceptionHandler The function to apply if the {@link Result} is {@link Fail}
     * @return The value or the result of the function applied to the exception
     */
    T recover(Function<Exception, T> exceptionHandler);

    /**
     * @return If the {@link Result} is {@link Ok}. This should be equivalent to {@code !isFail()}
     */
    boolean isSuccess();

    /**
     * @return If the {@link Result} is {@link Fail}. This should be equivalent to {@code !isSuccess()}
     */
    boolean isFailure();

    class Fail<T, E extends Exception> implements Result<T> {
        private final @NotNull E exception;

        private Fail(@NotNull E exception) {
            this.exception = exception;
        }

        @Override
        public @NotNull Optional<T> toOptional() {
            return Optional.empty();
        }

        @Override
        public @NotNull Optional<Exception> error() {
            return Optional.of(exception);
        }

        @Override
        public @NotNull Result<T> orElse(Supplier<Result<T>> supplier) {
            return supplier.get();
        }

        @Override
        public <R> @NotNull Result<R> flatMap(SafeFunction<T, Result<R>> function) {
            //noinspection unchecked
            return (Result<R>) this;
        }

        @Override
        public @NotNull <R> Result<R> map(SafeFunction<T, R> function) {
            //noinspection unchecked
            return (Result<R>) this;
        }

        @Override
        public @NotNull T getOrThrow() {
            Errors.sneakyThrow(exception);
            return null;
        }

        @Override
        public <R> R handle(Function<T, R> successHandler, Function<Exception, R> exceptionHandler) {
            return exceptionHandler.apply(exception);
        }

        @Override
        public T recover(Function<Exception, T> exceptionHandler) {
            return exceptionHandler.apply(exception);
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public boolean isFailure() {
            return true;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Fail<?, ?> fail = (Fail<?, ?>) o;
            return exception.equals(fail.exception);
        }

        @Override
        public int hashCode() {
            return Objects.hash(exception);
        }

        @Override
        public String toString() {
            return "Fail{" +
                    "exception=" + exception +
                    '}';
        }
    }

    class Ok<T> implements Result<T> {
        private final @NotNull T value;

        private Ok(@NotNull T value) {
            this.value = value;
        }

        @Override
        public @NotNull Optional<T> toOptional() {
            return Optional.of(value);
        }

        @Override
        public @NotNull Optional<Exception> error() {
            return Optional.empty();
        }

        @Override
        public @NotNull Result<T> orElse(Supplier<Result<T>> supplier) {
            return this;
        }

        @Override
        public <R> @NotNull Result<R> flatMap(SafeFunction<T, Result<R>> function) {
            return computeCatching(() -> function.apply(value));
        }

        @Override
        public @NotNull <R> Result<R> map(SafeFunction<T, R> function) {
            return runCatching(() -> function.apply(value));
        }

        @Override
        public @NotNull T getOrThrow() {
            return value;
        }

        @Override
        public <R> R handle(Function<T, R> successHandler, Function<Exception, R> exceptionHandler) {
            return successHandler.apply(value);
        }

        @Override
        public T recover(Function<Exception, T> exceptionHandler) {
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

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Ok<?> ok = (Ok<?>) obj;
            return Objects.equals(value, ok.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return "Ok{" +
                    "value=" + value +
                    '}';
        }
    }
}
