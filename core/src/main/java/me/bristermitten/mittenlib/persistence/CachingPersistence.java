package me.bristermitten.mittenlib.persistence;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import me.bristermitten.mittenlib.util.Unit;
import me.bristermitten.mittenlib.util.lambda.Functions;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

import static java.util.Optional.of;
import static java.util.concurrent.CompletableFuture.completedFuture;

public class CachingPersistence<I, T> implements Persistence<I, T> {
    private final Persistence<I, T> delegate;
    private final Function<T, I> idFunction;
    private final Cache<I, T> cache = createCache();

    public CachingPersistence(Persistence<I, T> delegate, Function<T, I> idFunction) {
        this.delegate = delegate;
        this.idFunction = idFunction;
    }

    protected Cache<I, T> createCache() {
        return CacheBuilder.newBuilder().build();
    }

    protected void addToCache(I id, T data) {
        cache.put(id, data);
    }

    protected void addToCache(T data) {
        cache.put(idFunction.apply(data), data);
    }

    @Override
    public @NotNull CompletableFuture<Unit> init() {
        return delegate.init().thenCompose(unit ->
                delegate.loadAll()
                        .thenApply(elements -> {
                            for (T element : elements) {
                                addToCache(idFunction.apply(element), element);
                            }
                            return Unit.UNIT;
                        }));
    }

    @Override
    public @NotNull CompletableFuture<Unit> save(@NotNull T value) {
        addToCache(value);
        return delegate.save(value);
    }

    @Override
    public @NotNull CompletableFuture<Optional<T>> load(@NotNull I id) {
        final T ifPresent = cache.getIfPresent(id);
        if (ifPresent != null) {
            return completedFuture(of((ifPresent)));
        }
        return lookup(id);
    }

    private CompletableFuture<Optional<T>> lookup(@NotNull I id) {
        return delegate.load(id)
                .whenComplete((o, t) -> {
                    if (t != null) {
                        throw new CompletionException(t);
                    }
                    o.ifPresent(this::addToCache);
                });
    }

    @Override
    public @NotNull CompletableFuture<Unit> delete(@NotNull I id) {
        cache.invalidate(id);
        return delegate.delete(id);
    }

    @Override
    public @NotNull CompletableFuture<Collection<T>> loadAll() {
        return delegate.loadAll()
                .whenComplete((o, t) -> {
                    if (t != null) {
                        throw new CompletionException(t);
                    }
                    o.forEach(this::addToCache);
                });
    }

    @Override
    public @NotNull CompletableFuture<Unit> saveAll(@NotNull Collection<T> values) {
        return CompletableFuture
                .allOf(values.stream().map(this::save).toArray(CompletableFuture[]::new))
                .thenCompose(Functions.constant(Unit.unitFuture()));
    }
}
