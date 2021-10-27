package me.bristermitten.mittenlib.util;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Strings {
    private Strings() {

    }

    /**
     * Join a collection of elements to a String using a given separator and toString function.
     * This is useful when wanting to join a collection of complex objects, whilst wanting to avoid the overhead
     * of Streams ({@link Collectors#joining()}}, and the boilerplate of a for loop
     *
     * @param collection The collection to join
     * @param toString   A function to transform an element to a String
     * @param separator  A separator for the joined string
     * @param <T>        The type of the collection
     * @return A joined string
     */
    public static <T> String joinWith(@NotNull Collection<T> collection, @NotNull Function<T, Object> toString, @NotNull String separator) {
        final StringJoiner stringJoiner = new StringJoiner(separator);
        for (T t : collection) {
            stringJoiner.add(toString.apply(t).toString());
        }
        return stringJoiner.toString();
    }
}
