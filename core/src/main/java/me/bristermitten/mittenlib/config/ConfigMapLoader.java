package me.bristermitten.mittenlib.config;

import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class ConfigMapLoader {
    private ConfigMapLoader() {
    }

    @SuppressWarnings("unused") // This is used in generated code
    public static <T> Optional<T> load(Map<String, Object> map,
                                       String key,
                                       Class<T> type,
                                       @Nullable T defaultValue,
                                       Function<Map<String, Object>, T> fromMap) {
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

    public static RuntimeException throwNotFound(String fieldName, String typeName, Class<?> enclosingClass, Object value) {
        return new IllegalArgumentException(
                String.format("Cannot deserialize complex type %s for field %s in class %s, received = %s", typeName, fieldName, enclosingClass, value));
    }
}
