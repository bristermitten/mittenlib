package me.bristermitten.mittenlib.util;

import org.jetbrains.annotations.Nullable;

public class Cast {
    private Cast() {

    }

    public static <T> @Nullable T safeCast(Object o, Class<T> type) {
        if (type.isInstance(o)) {
            //noinspection unchecked
            return (T) o;
        }
        return null;
    }
}
