package me.bristermitten.mittenlib.util.lambda;

import org.jetbrains.annotations.Contract;

/**
 * A function isomorphic to {@link java.util.function.Function}, but with the expectation that it is pure.
 * It is obviously impossible to enforce this, so this interface exists purely as a marker to indicate intent.
 * <p>
 * <b>Purity</b>: We define a pure function as one that, given the same input, will always return the same output,
 * and has no side effects (i.e., it does not modify any external state or interact with the outside world).
 *
 * @param <A> the input type
 * @param <B> the output type
 */
@FunctionalInterface
public interface PureFunction<A, B> {
    /**
     * Applies this pure function to the given argument.
     *
     * @param a the function argument
     * @return the function result
     */
    @Contract(pure = true)
    B apply(A a);
}
