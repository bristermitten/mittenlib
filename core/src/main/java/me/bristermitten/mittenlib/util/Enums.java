package me.bristermitten.mittenlib.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class for enums
 */
public class Enums {
    private Enums() {

    }

    /**
     * Creates a "prettified" name of an enum.
     * This is computed by removing underscores and capitalizing the first letter of each word, and then lowercasing the rest.
     *
     * @param e   The enum to get the name of
     * @param <E> The type of the enum
     * @return The prettified name of the enum
     */
    public static <E extends Enum<E>> @NotNull String prettyName(@NotNull E e) {
        final String[] split = e.name().toLowerCase().split("_");
        return Strings.joinWith(split, Strings::capitalize, " ");
    }


    /**
     * Retrieves an enum constant from the specified {@code enumClass} whose name matches the specified {@code name},
     * ignoring case sensitivity. If no match is found, returns {@code null}.
     *
     * @param name      the name of the enum constant to retrieve (case-insensitive).
     * @param enumClass the class of the enum type to search.
     * @param <E>       the type of the enum.
     * @return the matching enum constant, or {@code null} if no match is found.
     */
    public static <E extends Enum<E>> @Nullable E valueOfIgnoreCase(@NotNull String name, @NotNull Class<E> enumClass) {
        if (!enumClass.isEnum()) {
            throw new IllegalArgumentException("The specified class is not an enum!");
        }
        for (E e : enumClass.getEnumConstants()) { // can this be made faster?
            if (e.name().equalsIgnoreCase(name)) {
                return e;
            }
        }
        return null;
    }

    /**
     * Retrieves an enum constant from the specified {@code enumClass} whose name matches the given {@code name}.
     * If no constant is found, returns {@code null}.
     *
     * @param name      the name of the enum constant to retrieve.
     * @param enumClass the class of the enum type to search.
     * @param <E>       the type of the enum.
     * @return the matching enum constant, or {@code null} if no match is found.
     */
    public static <E extends Enum<E>> @Nullable E valueOfOrNull(@NotNull String name, @NotNull Class<E> enumClass) {
        try {
            return Enum.valueOf(enumClass, name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
