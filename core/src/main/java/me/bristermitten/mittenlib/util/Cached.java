package me.bristermitten.mittenlib.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * A {@link Supplier} which caches the value it supplies.
 *
 * @param <T> The type of the value.
 */
public class Cached<T> implements Supplier<T> {
    private final Supplier<T> computeWith;
    private @Nullable T t;

    /**
     * Create a new Cached with the given supplier, lazily computing the value.
     *
     * @param computeWith The supplier to compute the value with.
     */
    public Cached(@NotNull Supplier<@NotNull T> computeWith) {
        this(computeWith, false);
    }

    /**
     * Create a new Cached with the given supplier.
     *
     * @param computeWith The supplier to compute the value with.
     * @param eager       Whether to eagerly compute the value. If true, the supplier will be called immediately.
     */
    public Cached(Supplier<@NotNull T> computeWith, boolean eager) {
        this.computeWith = computeWith;
        if (eager) {
            update();
        }
    }

    /**
     * Invalidate the cached value, causing it to be recomputed on the next call to {@link #get()}.
     * Note that this does not respect {@link Cached#Cached(Supplier, boolean)}'s {@code eager} parameter,
     * and will always lazily compute the value.
     */
    public void invalidate() {
        t = null;
    }

    private void update() {
        t = computeWith.get();
    }

    /**
     * Get the cached value, computing it if it is not already cached.
     *
     * @return The cached value.
     */
    @Override
    @NotNull
    public T get() {
        if (t == null) {
            update();
        }
        return t;
    }

}
