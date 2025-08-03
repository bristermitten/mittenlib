package me.bristermitten.mittenlib.collections;

import net.jqwik.api.*;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.Map.Entry;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the MLImmutableMap interface using both unit tests and property-based testing
 */
class MLImmutableMapTest {

    /**
     * Create a concrete MLImmutableMap for testing purposes
     */
    private static <K, V> MLImmutableMap<K, V> createTestMap(Map<K, V> source) {
        // Use existing Maps implementation which should return MLImmutableMap instances
        if (source.isEmpty()) {
            // Create an empty map of the right type
            return Maps.of();
        }

        MLImmutableMap<K, V> map = Maps.of(source.entrySet());
        assertInstanceOf(MLImmutableMap.class, map, "Map should be an MLImmutableMap");
        return map;
    }

    @Test
    void testImmutability() {
        MLImmutableMap<String, Integer> map = createTestMap(Map.of("a", 1, "b", 2, "c", 3));

        // Test all mutating operations
        assertThrows(UnsupportedOperationException.class, () -> map.put("d", 4));
        assertThrows(UnsupportedOperationException.class, () -> map.remove("a"));
        assertThrows(UnsupportedOperationException.class, map::clear);
        assertThrows(UnsupportedOperationException.class, () -> map.put("d", 4));
        assertThrows(UnsupportedOperationException.class, () -> map.putIfAbsent("d", 4));
        assertThrows(UnsupportedOperationException.class, () -> map.replace("a", 10));
        assertThrows(UnsupportedOperationException.class, () -> map.replace("a", 1, 10));
        assertThrows(UnsupportedOperationException.class, () -> map.remove("a", 1));
        assertThrows(UnsupportedOperationException.class, () -> map.computeIfAbsent("d", k -> 4));
        assertThrows(UnsupportedOperationException.class, () -> map.computeIfPresent("a", (k, v) -> v + 1));
        assertThrows(UnsupportedOperationException.class, () -> map.compute("a", (k, v) -> v));
        assertThrows(UnsupportedOperationException.class, () -> map.merge("a", 10, Integer::sum));

        // Entry set should be immutable
        Set<Entry<String, Integer>> entrySet = map.entrySet();
        assertThrows(UnsupportedOperationException.class, () -> entrySet.add(Map.entry("d", 4)));
        assertThrows(UnsupportedOperationException.class, entrySet::clear);

        Iterator<Entry<String, Integer>> iterator = entrySet.iterator();
        iterator.next();
        assertThrows(UnsupportedOperationException.class, iterator::remove);

        // Key set should be immutable
        Set<String> keySet = map.keySet();
        assertThrows(UnsupportedOperationException.class, () -> keySet.add("d"));
        assertThrows(UnsupportedOperationException.class, () -> keySet.remove("a"));
        assertThrows(UnsupportedOperationException.class, keySet::clear);

        // Values collection should be immutable
        Collection<Integer> values = map.values();
        assertThrows(UnsupportedOperationException.class, () -> values.add(4));
        assertThrows(UnsupportedOperationException.class, () -> values.remove(1));
        assertThrows(UnsupportedOperationException.class, values::clear);
    }

    @Test
    void testContainsAndGet() {
        MLImmutableMap<String, Integer> map = createTestMap(Map.of("a", 1, "b", 2, "c", 3));

        // Test contains methods
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsKey("b"));
        assertTrue(map.containsKey("c"));
        assertFalse(map.containsKey("d"));

        assertTrue(map.containsValue(1));
        assertTrue(map.containsValue(2));
        assertTrue(map.containsValue(3));
        assertFalse(map.containsValue(4));

        // Test get method
        assertEquals(1, map.get("a"));
        assertEquals(2, map.get("b"));
        assertEquals(3, map.get("c"));
        assertNull(map.get("d"));
    }

    @Test
    void testGetOrDefault() {
        MLImmutableMap<String, Integer> map = createTestMap(Map.of("a", 1, "b", 2, "c", 3));

        // Test getOrDefault
        assertEquals(1, map.getOrDefault("a", 999));
        assertEquals(2, map.getOrDefault("b", 999));
        assertEquals(3, map.getOrDefault("c", 999));
        assertEquals(999, map.getOrDefault("d", 999));
    }

    @Test
    void testEmptyMap() {
        MLImmutableMap<String, Integer> emptyMap = createTestMap(Map.of());

        assertTrue(emptyMap.isEmpty());
        //noinspection ConstantValue
        assertEquals(0, emptyMap.size());
        assertEquals(Collections.emptySet(), emptyMap.keySet());
        assertEquals(Collections.emptySet(), emptyMap.entrySet());
        assertEquals(Collections.emptyList(), new ArrayList<>(emptyMap.values()));
    }

    @Test
    void testNullHandling() {
        // Constructing a map with null keys or values should throw
        assertThrows(NullPointerException.class, () -> createTestMap(Collections.singletonMap(null, 1)));
        assertThrows(NullPointerException.class, () -> createTestMap(Collections.singletonMap("a", null)));

        MLImmutableMap<String, Integer> map = createTestMap(Map.of("a", 1, "b", 2));

        // Operations with null should handle appropriately
        assertFalse(map.containsKey(null));
        assertFalse(map.containsValue(null));
        assertNull(map.get(null));
        assertEquals(999, map.getOrDefault(null, 999));
    }

    // Property-based tests

    @Property
    void mapContainsAllExpectedEntries(@ForAll("nonNullStringMap") Map<String, Integer> sourceMap) {
        MLImmutableMap<String, Integer> map = createTestMap(sourceMap);

        assertEquals(sourceMap.size(), map.size());

        for (Entry<String, Integer> entry : sourceMap.entrySet()) {
            assertTrue(map.containsKey(entry.getKey()));
            assertTrue(map.containsValue(entry.getValue()));
            assertEquals(entry.getValue(), map.get(entry.getKey()));
        }
    }

    @Property
    void mapEqualsStandardMap(@ForAll("nonNullStringMap") Map<String, Integer> sourceMap) {
        MLImmutableMap<String, Integer> map = createTestMap(sourceMap);

        // Map should equal a standard map with same entries
        assertEquals(sourceMap, map);
        assertEquals(sourceMap.hashCode(), map.hashCode());

        // Entry sets should be equal
        assertEquals(sourceMap.entrySet(), map.entrySet());

        // Key sets should be equal
        assertEquals(sourceMap.keySet(), map.keySet());

        // Values should be equal as collections (order may differ)
        assertEquals(new HashSet<>(sourceMap.values()), new HashSet<>(map.values()));
    }

    @Property
    void entrySetReflectsMapContents(@ForAll("nonNullStringMap") Map<String, Integer> sourceMap) {
        MLImmutableMap<String, Integer> map = createTestMap(sourceMap);
        Set<Entry<String, Integer>> entrySet = map.entrySet();

        assertEquals(map.size(), entrySet.size());

        for (Entry<String, Integer> entry : entrySet) {
            assertTrue(map.containsKey(entry.getKey()));
            assertEquals(entry.getValue(), map.get(entry.getKey()));
        }

        // Entry set should contain all entries from source map
        for (Entry<String, Integer> sourceEntry : sourceMap.entrySet()) {
            assertTrue(entrySet.contains(sourceEntry));
        }
    }

    @Provide
    Arbitrary<Map<String, Integer>> nonNullStringMap() {
        return Arbitraries.maps(
                        Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10),
                        Arbitraries.integers().between(-1000, 1000))
                .ofMaxSize(10);
    }
}
