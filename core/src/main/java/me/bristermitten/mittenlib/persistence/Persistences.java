package me.bristermitten.mittenlib.persistence;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static java.util.Optional.empty;

public interface Persistences<I, T, P extends Persistence<I, T>> {
    @NotNull
    default Optional<P> json() {
        return empty();
    }

    @NotNull
    default Optional<P> sqlite() {
        return empty();
    }

    @NotNull
    default Optional<P> mariadb() {
        return empty();
    }
}
