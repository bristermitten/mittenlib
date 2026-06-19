package me.bristermitten.mittenlib.util;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import org.jspecify.annotations.NullMarked;

/**
 * A value that can be exactly one of two possible types, encapsulated in a "left" and "right" value.
 * Unlike {@link Result}, both possibilities are expected to be "valid", i.e. there is no inherent notion of a
 * "success" or "failure" value.
 *
 * @param <L> The "Left" type
 * @param <R> The "Right" type
 */
@NullMarked
public interface Either<L, R> {

    static <L, R> Either<L, R> left(L left) {
        return new Left<>(left);
    }

    static <L, R> Either<L, R> right(R right) {
        return new Right<>(right);
    }

    Optional<L> left();

    Optional<R> right();

    <R2> Either<L, R2> mapRight(Function<R, R2> rFunction);

    <L2> Either<L2, R> mapLeft(Function<L, L2> lFunction);

    <L2, R2> Either<L2, R2> bimap(Function<L, L2> lFunction, Function<R, R2> rFunction);

    <V> V match(Function<L, V> leftFunction, Function<R, V> rightFunction);

    <R2> Either<L, R2> flatMapRight(Function<R, Either<L, R2>> mapper);

    <L2> Either<L2, R> flatMapLeft(Function<L, Either<L2, R>> mapper);

    static <R> Either<Exception, R> fromResult(Result<R> result) {
        return result.handle(Either::right, Either::left);
    }

    static <L extends Exception, R> Result<R> toResult(Either<L, R> either) {
        return either.match(Result::fail, Result::ok);
    }

    class Left<L, R> implements Either<L, R> {
        private final L left;

        private Left(L left) {
            this.left = Objects.requireNonNull(left, "Left value must not be null");
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Left) {
                return ((Left<?, ?>) obj).left.equals(this.left);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return left.hashCode();
        }

        @Override
        public String toString() {
            return "Left{" + "left=" + left + '}';
        }

        @Override
        public Optional<L> left() {
            return Optional.of(left);
        }

        @Override
        public Optional<R> right() {
            return Optional.empty();
        }

        @Override
        public <R2> Either<L, R2> mapRight(Function<R, R2> rFunction) {
            return new Left<>(this.left);
        }

        @Override
        public <L2> Either<L2, R> mapLeft(Function<L, L2> lFunction) {
            return new Left<>(lFunction.apply(this.left));
        }

        @Override
        public <L2, R2> Either<L2, R2> bimap(Function<L, L2> lFunction, Function<R, R2> rFunction) {
            return new Left<>(lFunction.apply(this.left));
        }

        @Override
        public <V> V match(Function<L, V> leftFunction, Function<R, V> rightFunction) {
            return leftFunction.apply(this.left);
        }

        @Override
        public <R2> Either<L, R2> flatMapRight(Function<R, Either<L, R2>> mapper) {
            return new Left<>(this.left);
        }

        @Override
        public <L2> Either<L2, R> flatMapLeft(Function<L, Either<L2, R>> mapper) {
            return mapper.apply(this.left);
        }
    }

    class Right<L, R> implements Either<L, R> {
        private final R right;

        private Right(R right) {
            this.right = Objects.requireNonNull(right, "Right value must not be null");
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Right) {
                return ((Right<?, ?>) obj).right.equals(this.right);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return right.hashCode();
        }

        @Override
        public String toString() {
            return "Right{" + "right=" + right + '}';
        }

        @Override
        public Optional<L> left() {
            return Optional.empty();
        }

        @Override
        public Optional<R> right() {
            return Optional.of(right);
        }

        @Override
        public <R2> Either<L, R2> mapRight(Function<R, R2> rFunction) {
            return new Right<>(rFunction.apply(this.right));
        }

        @Override
        public <L2> Either<L2, R> mapLeft(Function<L, L2> lFunction) {
            return new Right<>(this.right);
        }

        @Override
        public <L2, R2> Either<L2, R2> bimap(Function<L, L2> lFunction, Function<R, R2> rFunction) {
            return new Right<>(rFunction.apply(this.right));
        }

        @Override
        public <V> V match(Function<L, V> leftFunction, Function<R, V> rightFunction) {
            return rightFunction.apply(this.right);
        }

        @Override
        public <R2> Either<L, R2> flatMapRight(Function<R, Either<L, R2>> mapper) {
            return mapper.apply(this.right);
        }

        @Override
        public <L2> Either<L2, R> flatMapLeft(Function<L, Either<L2, R>> mapper) {
            return new Right<>(this.right);
        }
    }
}
