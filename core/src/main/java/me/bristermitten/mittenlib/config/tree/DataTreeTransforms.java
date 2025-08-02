package me.bristermitten.mittenlib.config.tree;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for transforming between DataTree and POJO representations.
 * This class provides methods to load a DataTree from various object types
 * and to convert a DataTree back to a POJO.
 */
public class DataTreeTransforms {
    /**
     * Load a DataTree from an object.
     * This method can handle various types of objects including
     * {@code null}, {@link DataTree}, {@link Double}, {@link Number}, {@link String}, {@link Boolean}, {@link Map}, and {@link List}.
     * Any other type of object will throw an {@link IllegalArgumentException}.
     * The method will return a {@link DataTree.DataTreeNull} instance if the input is {@code null}.
     *
     * @param node the object to load the DataTree from, can be {@code null}
     * @return a DataTree representation of the input object
     */
    public static @NotNull DataTree loadFrom(@Nullable Object node) {
        if (node == null) return DataTree.DataTreeNull.INSTANCE;
        if (node instanceof DataTree) {
            return (DataTree) node;
        }
        if (node instanceof Double) {
            return new DataTree.DataTreeLiteral.DataTreeLiteralFloat((Double) node);
        }
        if (node instanceof Number) {
            return new DataTree.DataTreeLiteral.DataTreeLiteralInt(((Number) node).longValue());
        }
        if (node instanceof String) {
            return new DataTree.DataTreeLiteral.DataTreeLiteralString((String) node);
        }
        if (node instanceof Boolean) {
            return new DataTree.DataTreeLiteral.DataTreeLiteralBoolean((Boolean) node);
        }

        if (node instanceof Map) {
            LinkedHashMap<DataTree, DataTree> map = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) node).entrySet()) {
                map.put(loadFrom(entry.getKey()), loadFrom(entry.getValue()));
            }
            return new DataTree.DataTreeMap(map);
        }
        if (node instanceof List) {
            DataTree[] treeList = new DataTree[((List<?>) node).size()];
            List<?> objects = (List<?>) node;
            for (int i = 0; i < objects.size(); i++) {
                Object o = objects.get(i);
                treeList[i] = loadFrom(o);
            }
            return new DataTree.DataTreeArray(treeList);
        }
        throw new IllegalArgumentException("Unknown type: " + node.getClass());
    }

    /**
     * Convert a DataTree to a POJO representation, as an inverse of {@link #loadFrom(Object)}.
     *
     * @param tree the DataTree to convert, must not be {@code null}
     * @return the POJO representation of the DataTree, or {@code null} if the DataTree is an instance of {@link DataTree.DataTreeNull}
     */
    public static @Nullable Object toPOJO(@NotNull DataTree tree) {
        if (tree instanceof DataTree.DataTreeNull) {
            return null;
        }
        if (tree instanceof DataTree.DataTreeLiteral) {
            return tree.value();
        }
        if (tree instanceof DataTree.DataTreeMap) {
            Map<DataTree, DataTree> map = ((DataTree.DataTreeMap) tree).values();
            Map<Object, Object> pojoMap = new LinkedHashMap<>();
            map.forEach((key, value) -> pojoMap.put(toPOJO(key), toPOJO(value)));
            return pojoMap;
        }
        if (tree instanceof DataTree.DataTreeArray) {
            DataTree[] list = ((DataTree.DataTreeArray) tree).getValues();
            List<Object> pojoList = new ArrayList<>();
            for (DataTree dataTree : list) {
                pojoList.add(toPOJO(dataTree));
            }
            return pojoList;
        }
        throw new IllegalArgumentException("Unknown type: " + tree.getClass());
    }
}
