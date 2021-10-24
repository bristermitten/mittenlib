package me.bristermitten.mittenlib.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class Cached<T> implements Supplier<T> {
    private final Supplier<T> computeWith;
    private @Nullable T t;


    public Cached(Supplier<@NotNull T> computeWith) {
        this(computeWith, false);
    }

    public Cached(Supplier<@NotNull T> computeWith, boolean eager) {
        this.computeWith = computeWith;
        if (eager) {
            update();
        }
    }

    public void invalidate() {
        t = null;
    }

    private void update() {
        t = computeWith.get();
    }

    @Override
    @NotNull
    public T get() {
        if (t == null) {
            update();
        }
        return t;
    }

}
