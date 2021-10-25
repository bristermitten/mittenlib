package me.bristermitten.mittenlib.util.lambda;

import java.util.function.Function;

public class Functions {
    private Functions() {

    }

    public static <A, R> Function<A, R> constant(R r) {
        return unused -> r;
    }
}
