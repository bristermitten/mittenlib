package me.bristermitten.mittenlib.persistence;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface Persistences<I, T, P extends Persistence<I, T>> {
    @NotNull Optional<P> json();

    @NotNull Optional<P> sqlite();

    @NotNull Optional<P> mariadb();

}
