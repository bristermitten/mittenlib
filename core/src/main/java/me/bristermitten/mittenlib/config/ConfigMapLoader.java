package me.bristermitten.mittenlib.config;

import com.google.errorprone.annotations.InlineMe;
import me.bristermitten.mittenlib.config.exception.ConfigLoadingErrors;
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
     * Creates an exception to use when a value cannot be deserialized as it is not found (i.e. is null)
     *
     * @param fieldName      the name of the field that is trying to be deserialized
     * @param typeName       the name of the type that is trying to be deserialized
     * @param enclosingClass the name of the enclosing class
     * @return the exception to throw
     * @deprecated Use {@link ConfigLoadingErrors#notFoundException(String, String, Class, String)}
     */
    @Deprecated
    @InlineMe(replacement = "ConfigLoadingErrors.notFoundException(fieldName, typeName, enclosingClass, keyName)", imports = "me.bristermitten.mittenlib.config.exception.ConfigLoadingErrors")
    public static RuntimeException throwNotFound(String fieldName, String typeName, Class<?> enclosingClass, String keyName) {
        return ConfigLoadingErrors.notFoundException(fieldName, typeName, enclosingClass, keyName);
    }
}
