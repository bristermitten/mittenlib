package me.bristermitten.mittenlib.config;

import com.google.gson.reflect.TypeToken;
import me.bristermitten.mittenlib.config.tree.DataTree;
import me.bristermitten.mittenlib.util.MultipleFailuresException;
import me.bristermitten.mittenlib.util.Result;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Utility for deserializing collections using the MittenLib config system.
 * This is used by generated config classes.
 */
public class CollectionsUtils {
    private static final TypeToken<List<DataTree>> LIST_MAP_STRING_OBJECT_TOKEN = new TypeToken<List<DataTree>>() {
    };
    private static final TypeToken<DataTree> MAP_STRING_OBJECT_TOKEN = new TypeToken<DataTree>() {
    };


    private CollectionsUtils() {

    }

    /**
     * Attempt to deserialize a list using the MittenLib config system.
     *
     * @param rawData                 the raw data to deserialize, which should represent a {@code List<Map<String, Object>>}
     * @param baseContext             the base context to use for deserialization
     * @param deserializationFunction the function to use for turning a {@link Map} into an {@link T}
     * @param <T>                     the type to deserialize to
     * @return a {@link Result} containing the deserialized list, or a {@link Result#fail(Exception)} if deserialization failed
     */
    public static <T> Result<List<T>> deserializeList(Object rawData, DeserializationContext baseContext, DeserializationFunction<T> deserializationFunction) {

        try {
            // gamble: first we try a blind cast and hope for the best
            //noinspection unchecked
            return deserialiseListFrom((List<DataTree>) rawData, baseContext, deserializationFunction);
        } catch (ClassCastException ignore) {
        }
        // fall back to gson for a more informative error message
        Result<List<DataTree>> rawListRes = baseContext.getMapper().map(rawData, LIST_MAP_STRING_OBJECT_TOKEN);
        return rawListRes.flatMap(f -> deserialiseListFrom(f, baseContext, deserializationFunction));
    }


    private static <T> Result<List<T>> deserialiseListFrom(List<DataTree> rawList, DeserializationContext baseContext, DeserializationFunction<T> deserializationFunction) {
        // Apply the deserialization function to each element of the list, flattening the result
        final List<T> res = new ArrayList<>();
        final List<Throwable> errors = new ArrayList<>();

        for (DataTree map : rawList) {
            Result<T> deserialised = deserializationFunction.apply(baseContext.withData(map));
            deserialised.error().ifPresent(errors::add);
            deserialised.value().ifPresent(res::add);
        }
        if (!errors.isEmpty()) {
            return Result.fail(new MultipleFailuresException("Failed to deserialize list", errors));
        }

        return Result.ok(res);
    }

    /**
     * Attempt to deserialise a map using the MittenLib config system.
     *
     * @param keyType                 the type of the keys in the map
     * @param rawData                 the raw data to deserialise, which should represent a {@code Map<String, Object>}
     * @param baseContext             the base context to use for deserialization
     * @param deserializationFunction the function to use for turning a {@link Map} into an {@link V}
     * @param <K>                     the type of the keys in the map
     * @param <V>                     the type to deserialise to
     * @return a {@link Result} containing the deserialised map, or a {@link Result#fail(Exception)} if deserialisation failed
     */
    public static <K, V> Result<Map<K, V>> deserializeMap(Class<K> keyType, Object rawData, DeserializationContext baseContext, DeserializationFunction<V> deserializationFunction) {
        // first we use the object mapper to convert the raw data into a Map<K, Map<String, Object>>
//        noinspection unchecked absolutely evil
        Result<Map<K, DataTree>> rawMapRes = baseContext.getMapper().map(rawData,
                (TypeToken<Map<K, DataTree>>) TypeToken.get(
                        new GenericParameterizedType(Map.class, keyType, MAP_STRING_OBJECT_TOKEN.getType())
                ));
        return rawMapRes.flatMap(rawMap -> {
            // Apply the deserialization function to each value in the map, flattening the result
            final Map<K, V> res = new HashMap<>();
            final List<Throwable> errors = new ArrayList<>();
            for (Map.Entry<K, DataTree> entry : rawMap.entrySet()) {
                Result<V> deserialised = deserializationFunction.apply(baseContext.withData(entry.getValue()));
                deserialised.error().ifPresent(errors::add);
                deserialised.value().ifPresent(value -> res.put(entry.getKey(), value));
            }
            if (!errors.isEmpty()) {
                return Result.fail(new MultipleFailuresException("Failed to deserialize map", errors));
            }

            return Result.ok(res);
        });
    }

    /**
     * A custom implementation of {@link ParameterizedType} that allows for defining a parameterized type at runtime.
     * This class represents a parameterized type with a raw type and its actual type arguments.
     * For example, we can use this to represent a type like {@code Map<String, List<MyType>>} dynamically using {@code new GenericParameterizedType(Map.class, String.class, new GenericParameterizedType(List.class, MyType.class))}
     */
    static class GenericParameterizedType implements ParameterizedType {
        private @NotNull
        final Class<?> container;
        private @NotNull
        final Type @NotNull [] wrapped;

        @Contract(pure = true)
        public GenericParameterizedType(@NotNull Class<?> container, @NotNull Type @NotNull ... wrapped) {
            this.container = container;
            this.wrapped = wrapped;
        }

        @Override
        public Type @NotNull [] getActualTypeArguments() {
            return this.wrapped;
        }

        @Override
        @NotNull
        public Type getRawType() {
            return this.container;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }
    }
}
