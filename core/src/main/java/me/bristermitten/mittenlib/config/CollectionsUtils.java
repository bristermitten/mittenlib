package me.bristermitten.mittenlib.config;

import com.google.gson.reflect.TypeToken;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import me.bristermitten.mittenlib.config.tree.DataTree;
import me.bristermitten.mittenlib.config.tree.DataTreeTransforms;
import me.bristermitten.mittenlib.util.MultipleFailuresException;
import me.bristermitten.mittenlib.util.Result;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

/**
 * Utility for deserializing collections using the MittenLib config system. This is used by
 * generated config classes.
 */
public class CollectionsUtils {
    private static final TypeToken<List<DataTree>> LIST_MAP_STRING_OBJECT_TOKEN = new TypeToken<List<DataTree>>() {};
    private static final TypeToken<DataTree> MAP_STRING_OBJECT_TOKEN = new TypeToken<DataTree>() {};

    private CollectionsUtils() {}

    /**
     * Attempt to deserialize a list using the MittenLib config system.
     *
     * @param rawData                 the raw data to deserialize, which should represent a {@code List<Map<String,
     *                                Object>>}
     * @param baseContext             the base context to use for deserialization
     * @param deserializationFunction the function to use for turning a {@link Map} into an {@link T}
     * @param <T>                     the type to deserialize to
     * @return a {@link Result} containing the deserialized list, or a {@link Result#fail(Exception)}
     * if deserialization failed
     */
    public static <T> Result<List<T>> deserializeList(
            Object rawData, DeserializationContext baseContext, DeserializationFunction<T> deserializationFunction) {

        if (rawData instanceof DataTree.DataTreeArray) {
            List<DataTree> rawList = ((DataTree.DataTreeArray) rawData).value();
            return deserializeListFrom(rawList, baseContext, deserializationFunction);
        }
        // fall back to gson for a more informative error message
        Result<List<DataTree>> rawListRes = baseContext.getMapper().map(rawData, LIST_MAP_STRING_OBJECT_TOKEN);
        return rawListRes.flatMap(f -> deserializeListFrom(f, baseContext, deserializationFunction));
    }

    private static <T> Result<List<T>> deserializeListFrom(
            List<DataTree> rawList,
            DeserializationContext baseContext,
            DeserializationFunction<T> deserializationFunction) {
        // Apply the deserialization function to each element of the list, flattening the result
        final List<T> res = new ArrayList<>();
        final List<Throwable> errors = new ArrayList<>();

        for (DataTree map : rawList) {
            Result<T> deserialized = deserializationFunction.apply(baseContext.withData(map));
            if (deserialized.isFailure()) {
                deserialized.error().ifPresent(errors::add);
            } else {
                res.add(deserialized.getOrThrow());
            }
        }
        if (!errors.isEmpty()) {
            return Result.fail(new MultipleFailuresException("Failed to deserialize list", errors));
        }

        return Result.ok(res);
    }

    /**
     * Attempt to deserialize a set using the MittenLib config system.
     *
     * @param rawData                 the raw data to deserialize
     * @param baseContext             the base context to use for deserialization
     * @param deserializationFunction the function to use for turning a {@link Map} into an {@link T}
     * @param <T>                     the type to deserialize to
     * @return a {@link Result} containing the deserialized set, or a {@link Result#fail(Exception)}
     * if deserialization failed
     */
    public static <T> Result<Set<T>> deserializeSet(
            Object rawData, DeserializationContext baseContext, DeserializationFunction<T> deserializationFunction) {
        return deserializeList(rawData, baseContext, deserializationFunction).map(LinkedHashSet::new);
    }

    /**
     * Attempt to deserialize a map using the MittenLib config system.
     *
     * @param keyType                 the type of the keys in the map
     * @param rawData                 the raw data to deserialize, which should represent a {@code Map<String,
     *                                Object>}
     * @param baseContext             the base context to use for deserialization
     * @param deserializationFunction the function to use for turning a {@link Map} into an {@link V}
     * @param <K>                     the type of the keys in the map
     * @param <V>                     the type to deserialize to
     * @return a {@link Result} containing the deserialized map, or a {@link Result#fail(Exception)}
     * if deserialization failed
     */
    public static <K, V> Result<Map<K, V>> deserializeMap(
            Class<K> keyType,
            Object rawData,
            DeserializationContext baseContext,
            DeserializationFunction<V> deserializationFunction) {

        // fast path: if rawData is a DataTree already, we can iterate and deserialize its entries directly
        if (rawData instanceof DataTree.DataTreeMap) {
            Map<DataTree, DataTree> rawMap = ((DataTree.DataTreeMap) rawData).values();
            return deserializeMapFrom(keyType, rawMap, baseContext, deserializationFunction);
        }
        // fallback to Gson if it's not a DataTree
        //noinspection unchecked bad
        Result<Map<K, DataTree>> rawMapRes = baseContext.getMapper().map(rawData, (TypeToken<Map<K, DataTree>>)
                TypeToken.get(new GenericParameterizedType(Map.class, keyType, MAP_STRING_OBJECT_TOKEN.getType())));

        return rawMapRes.flatMap(rawMap -> {
            Map<DataTree, DataTree> treeMap = new HashMap<>();
            // turn keys and values back into DataTrees and then send to the helper
            for (Map.Entry<K, DataTree> entry : rawMap.entrySet()) {
                treeMap.put(DataTreeTransforms.loadFrom(entry.getKey()), entry.getValue());
            }
            return deserializeMapFrom(keyType, treeMap, baseContext, deserializationFunction);
        });
    }

    private static <K, V> Result<Map<K, V>> deserializeMapFrom(
            Class<K> keyType,
            Map<DataTree, DataTree> rawMap,
            DeserializationContext baseContext,
            DeserializationFunction<V> deserializationFunction) {
        final Map<K, V> res = new HashMap<>();
        final List<Throwable> errors = new ArrayList<>();

        for (Map.Entry<DataTree, DataTree> entry : rawMap.entrySet()) {
            Result<K> keyResult = parseKey(keyType, entry.getKey(), baseContext);

            // deserialize the value
            Result<V> valueResult = deserializationFunction.apply(baseContext.withData(entry.getValue()));
            keyResult.error().ifPresent(errors::add);
            valueResult.error().ifPresent(errors::add);

            if (keyResult.isSuccess() && valueResult.isSuccess()) {
                res.put(keyResult.getOrThrow(), valueResult.getOrThrow());
            }
        }
        if (!errors.isEmpty()) {
            return Result.fail(new MultipleFailuresException("Failed to deserialize map", errors));
        }
        return Result.ok(res);
    }

    @SuppressWarnings("unchecked")
    private static <K> Result<K> parseKey(Class<K> keyType, DataTree keyTree, DeserializationContext baseContext) {
        if (!(keyTree instanceof DataTree.DataTreeLiteral)) {
            return baseContext.getMapper().map(keyTree, TypeToken.get(keyType));
        }

        if (keyType == String.class && keyTree instanceof DataTree.DataTreeLiteral.DataTreeLiteralString) {
            return Result.ok((K) ((DataTree.DataTreeLiteral.DataTreeLiteralString) keyTree).value());
        }

        if (keyType == Integer.class) {
            if (keyTree instanceof DataTree.DataTreeLiteral.DataTreeLiteralInt) {
                return Result.ok((K) ((DataTree.DataTreeLiteral.DataTreeLiteralInt) keyTree).value());
            }
            if (keyTree instanceof DataTree.DataTreeLiteral.DataTreeLiteralString) {
                try {
                    return Result.ok(
                            (K) Integer.valueOf(((DataTree.DataTreeLiteral.DataTreeLiteralString) keyTree).value()));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        if (keyType == Double.class) {
            if (keyTree instanceof DataTree.DataTreeLiteral.DataTreeLiteralFloat) {
                return Result.ok((K) ((DataTree.DataTreeLiteral.DataTreeLiteralFloat) keyTree).value());
            }
            if (keyTree instanceof DataTree.DataTreeLiteral.DataTreeLiteralString) {
                try {
                    return Result.ok(
                            (K) Double.valueOf(((DataTree.DataTreeLiteral.DataTreeLiteralString) keyTree).value()));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        if (keyType == Boolean.class && keyTree instanceof DataTree.DataTreeLiteral.DataTreeLiteralBoolean) {
            return Result.ok((K) ((DataTree.DataTreeLiteral.DataTreeLiteralBoolean) keyTree).value());
        }

        return baseContext.getMapper().map(keyTree, TypeToken.get(keyType));
    }

    /**
     * A custom implementation of {@link ParameterizedType} that allows for defining a parameterized
     * type at runtime. This class represents a parameterized type with a raw type and its actual type
     * arguments. For example, we can use this to represent a type like {@code Map<String,
     * List<MyType>>} dynamically using {@code new GenericParameterizedType(Map.class, String.class,
     * new GenericParameterizedType(List.class, MyType.class))}
     */
    static class GenericParameterizedType implements ParameterizedType {
        private final Class<?> container;
        private final Type[] wrapped;

        @Contract(pure = true)
        public GenericParameterizedType(Class<?> container, Type... wrapped) {
            this.container = container;
            this.wrapped = wrapped;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return this.wrapped;
        }

        @Override
        public Type getRawType() {
            return this.container;
        }

        @Override
        @Nullable public Type getOwnerType() {
            return null;
        }
    }
}
