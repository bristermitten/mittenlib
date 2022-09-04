package me.bristermitten.mittenlib.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class for casting objects
 */
public class Cast {
    private Cast() {

    }

    /**
     * Safely cast an object to a type, returning null if the cast fails or the object is null
     *
     * @param o    the object to cast
     * @param type the type to cast to
     * @param <T>  the type to cast to
     * @return the cast object, or null if the cast failed or the object was null
     */
    @Contract("null, _ -> null")
    @Nullable
    public static <T> T safeCast(@Nullable Object o, Class<T> type) {
        if (type.isInstance(o)) {
            //noinspection unchecked
            return (T) o;
        }
        return null;
    }
}
