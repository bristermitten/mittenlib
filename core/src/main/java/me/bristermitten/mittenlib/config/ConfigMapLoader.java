package me.bristermitten.mittenlib.config;

import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Used in generated code to help with the loading of config maps
 */
public class ConfigMapLoader {
    private ConfigMapLoader() {
    }

    /**
     * Get a value from a map, or return an empty optional if the value is null
     * This tries a few different ways to load the value:
     * <ol>
     *     <li>Cast if it is an instance of {@code type}</li>
     *     <li>If it is a {@link Map}, use {@code fromMap}</li>
     *     <li>Use the default value, if present</li>
     * </ol>
     * If all of them fail, an {@link IllegalArgumentException} is thrown
     *
     * @param map          the map to get the value from
     * @param key          the key to get the value with
     * @param type         the type to cast the value to
     * @param defaultValue the default value to use if the value is null
     * @param fromMap      the function to use to load the value from a map structure
     * @param <T>          the type to cast the value to
     * @return the value, or an empty optional if the value is null
     * @throws IllegalArgumentException if all the ways to load the value fail
     */
    @SuppressWarnings("unused") // This is used in generated code
    public static <T> Optional<T> load(Map<String, Object> map,
                                       String key,
                                       Class<T> type,
                                       @Nullable T defaultValue,
                                       Function<Map<String, Object>, T> fromMap) throws IllegalArgumentException {
        Object value = map.get(key);

        if (type.isInstance(value)) {
            //noinspection unchecked
            return Optional.of((T) value);
        }
        if (value instanceof Map) {
            //noinspection unchecked
            return Optional.of(fromMap.apply((Map<String, Object>) value));
        }
        if (defaultValue != null) {
            return Optional.of(defaultValue);
        }
        throw new IllegalArgumentException(String.format("Cannot deserialize %s from map %s with key %s", type, map, key));
    }

    /**
     * Creates an exception to use when a value cannot be deserialized as it is not found
     *
     * @param fieldName      the name of the field that is trying to be deserialized
     * @param typeName       the name of the type that is trying to be deserialized
     * @param enclosingClass the name of the enclosing class
     * @param value          the value that was found instead of the expected value
     * @return the exception to throw
     */
    public static RuntimeException throwNotFound(String fieldName, String typeName, Class<?> enclosingClass, Object value) {
        return new IllegalArgumentException(
                String.format("Cannot deserialize complex type %s for field %s in class %s, received = %s", typeName, fieldName, enclosingClass, value));
    }
}
