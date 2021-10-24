package me.bristermitten.mittenlib.persistence;

import me.bristermitten.mittenlib.util.Unit;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface Persistence<I, T> {
    @NotNull CompletableFuture<Unit> save(@NotNull T value);

    @NotNull CompletableFuture<Optional<T>> load(@NotNull I id);

    @NotNull CompletableFuture<Unit> delete(@NotNull I id);

    @NotNull CompletableFuture<Collection<T>> loadAll();

    @NotNull CompletableFuture<Unit> saveAll(@NotNull Collection<T> values);
}
