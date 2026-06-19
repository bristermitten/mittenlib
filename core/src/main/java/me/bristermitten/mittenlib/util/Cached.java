package me.bristermitten.mittenlib.util;

import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/**
 * A {@link Supplier} which caches the value it supplies.
 * This value is NOT thread-safe.
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
    public Cached(Supplier<T> computeWith) {
        this(computeWith, false);
    }

    /**
     * Create a new Cached with the given supplier.
     *
     * @param computeWith The supplier to compute the value with.
     * @param eager Whether to eagerly compute the value. If true, the supplier will be called
     *     immediately.
     */
    public Cached(Supplier<T> computeWith, boolean eager) {
        this.computeWith = computeWith;
        if (eager) {
            update();
        }
    }

    /**
     * Invalidate the cached value, causing it to be recomputed on the next call to {@link #get()}.
     * Note that this does not respect {@link Cached#Cached(Supplier, boolean)}'s {@code eager}
     * parameter, and will always lazily compute the value.
     */
    public void invalidate() {
        t = null;
    }

    private T update() {
        return t = computeWith.get();
    }

    /**
     * Get the cached value, computing it if it is not already cached.
     *
     * @return The cached value.
     */
    @Override
    public T get() {
        if (t == null) {
            return update();
        }
        return t;
    }
}
