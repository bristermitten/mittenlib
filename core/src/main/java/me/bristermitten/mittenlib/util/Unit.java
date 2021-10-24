package me.bristermitten.mittenlib.util;

import java.util.concurrent.CompletableFuture;

/**
 * Unit value
 */
public class Unit {
    public static final Unit UNIT = new Unit();

    private Unit() {

    }

    public static CompletableFuture<Unit> unitFuture() {
        return CompletableFuture.completedFuture(UNIT);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Unit;
    }

    @Override
    public int hashCode() {
        return 1;
    }

    @Override
    public String toString() {
        return "Unit";
    }
}
