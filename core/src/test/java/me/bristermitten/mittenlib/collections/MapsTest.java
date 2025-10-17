package me.bristermitten.mittenlib.collections;

import net.jqwik.api.*;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class MapsTest {

    @Test
    void assertThat_singletonMap_creationWorks() {
        final Map<String, String> map = Maps.of("hello", "world");
        assertEquals(1, map.size());
        assertEquals("world", map.get("hello"));
    }

    @Test
    void assertThat_singletonMap_equalityWorks() {
        final Map<String, String> maps = Maps.of("hello", "world");
        final Map<String, String> hashMap = new HashMap<>();
        hashMap.put("hello", "world");
        assertEquals(hashMap, maps);
        assertEquals(maps, hashMap);
    }

    @Test
    void assertThat_singletonMap_containsWorks() {
        final Map<String, String> maps = Maps.of("hello", "world");
        assertTrue(maps.containsKey("hello"));
        assertTrue(maps.containsValue("world"));
        assertFalse(maps.containsKey("hello_world"));
        assertFalse(maps.containsValue("hello"));
    }

    @Test
    void assertThat_map2_creationWorks() {
        final Map<String, String> maps = Maps.of("hello", "world", "a", "b");
        assertEquals(2, maps.size());
        assertEquals("world", maps.get("hello"));
        assertEquals("b", maps.get("a"));
    }

    @Test
    void assertThat_map2_equalityWorks() {
        final Map<String, String> maps = Maps.of("hello", "world", "a", "b");
        final Map<String, String> hashMap = new HashMap<>();
        hashMap.put("hello", "world");
        hashMap.put("a", "b");
        assertEquals(hashMap, maps);
        assertEquals(maps, hashMap);
    }

    @Test
    void assertThat_map3_creationWorks() {
        final Map<String, String> maps = Maps.of("hello", "world", "a", "b", "c", "d");
        assertEquals(3, maps.size());
        assertEquals("world", maps.get("hello"));
        assertEquals("b", maps.get("a"));
        assertEquals("d", maps.get("c"));
    }

    @Test
    void assertThat_largeMap_creationWorks() {
        final Map<String, String> maps = Maps.of(
                "k1", "v1", "k2", "v2", "k3", "v3", "k4", "v4", "k5", "v5",
                "k6", "v6", "k7", "v7", "k8", "v8", "k9", "v9", "k10", "v10"
        );
        assertEquals(10, maps.size());
        for (int i = 1; i <= 10; i++) {
            assertEquals("v" + i, maps.get("k" + i));
        }
    }

    @Test
    void testMapFromEntries() {
        Entry<String, Integer> entry1 = Maps.entry("a", 1);
        Entry<String, Integer> entry2 = Maps.entry("b", 2);
        Entry<String, Integer> entry3 = Maps.entry("c", 3);

        Map<String, Integer> map = Maps.of(entry1, entry2, entry3);
        assertEquals(3, map.size());
        assertEquals(1, map.get("a"));
        assertEquals(2, map.get("b"));
        assertEquals(3, map.get("c"));
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    void testImmutabilityOperations() {
        Map<String, Integer> map = Maps.of("key", 42);

        assertThrows(UnsupportedOperationException.class, () -> map.put("new", 99));
        assertThrows(UnsupportedOperationException.class, () -> map.remove("key"));
        assertThrows(UnsupportedOperationException.class, map::clear);
        assertThrows(UnsupportedOperationException.class, () -> map.put("a", 1));
        assertThrows(UnsupportedOperationException.class, () -> map.putIfAbsent("new", 99));
        assertThrows(UnsupportedOperationException.class, () -> map.replace("key", 99));
        assertThrows(UnsupportedOperationException.class, () -> map.replace("key", 42, 99));
        assertThrows(UnsupportedOperationException.class, () -> map.remove("key", 42));
        assertThrows(UnsupportedOperationException.class, () -> map.computeIfAbsent("new", k -> 99));
        assertThrows(UnsupportedOperationException.class, () -> map.computeIfPresent("key", (k, v) -> 99));
        assertThrows(UnsupportedOperationException.class, () -> map.compute("key", (k, v) -> 99));
    }

    @Test
    void testEntrySetImmutability() {
        Map<String, Integer> map = Maps.of("key", 42);
        Set<Entry<String, Integer>> entrySet = map.entrySet();

        assertThrows(UnsupportedOperationException.class, () ->
                entrySet.add(Maps.entry("new", 99)));
        assertThrows(UnsupportedOperationException.class, () ->
                entrySet.remove(Maps.entry("key", 42)));
        assertThrows(UnsupportedOperationException.class, entrySet::clear);
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    void testNullKeyThrows() {
        assertThrows(NullPointerException.class, () -> Maps.of(null, 1));
        assertThrows(NullPointerException.class, () -> Maps.of("a", 1, null, 2));
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    void testNullValueThrows() {
        assertThrows(NullPointerException.class, () -> Maps.of("a", null));
        assertThrows(NullPointerException.class, () -> Maps.of("a", 1, "b", null));
    }

    @Test
    void testDuplicateKeysThrows() {
        assertThrows(IllegalArgumentException.class, () -> Maps.of("a", 1, "a", 2));
    }

    @Test
    void testMapWithComplexObjects() {
        List<String> value1 = List.of("a", "b", "c");
        List<String> value2 = List.of("x", "y", "z");

        Map<String, List<String>> map = Maps.of("list1", value1, "list2", value2);

        assertEquals(2, map.size());
        assertEquals(value1, map.get("list1"));
        assertEquals(value2, map.get("list2"));
    }

    // Property-based tests using jqwik

    @Property
    void mapContainsAllEntries(@ForAll List<Entry<String, Integer>> entries) {
        // Create a map with unique keys
        Map<String, Integer> uniqueKeyMap = new HashMap<>();
        for (Entry<String, Integer> entry : entries) {
            uniqueKeyMap.put(entry.getKey(), entry.getValue());
        }

        Map<String, Integer> map = Maps.of(uniqueKeyMap.entrySet());

        assertEquals(uniqueKeyMap.size(), map.size());

        for (String key : uniqueKeyMap.keySet()) {
            assertEquals(uniqueKeyMap.get(key), map.get(key));
            assertTrue(map.containsKey(key));
            assertTrue(map.containsValue(uniqueKeyMap.get(key)));
        }
    }

    @Property
    void mapEqualsStandardMap(@ForAll Map<String, Integer> standardMap) {
        // Convert to our Maps implementation
        List<Entry<String, Integer>> entries = new ArrayList<>(standardMap.entrySet());

        Map<String, Integer> ourMap = Maps.of(entries);

        assertEquals(standardMap, ourMap);
        assertEquals(ourMap, standardMap);
        assertEquals(standardMap.hashCode(), ourMap.hashCode());
        assertEquals(standardMap.keySet(), ourMap.keySet());
        assertEquals(new HashSet<>(standardMap.values()), new HashSet<>(ourMap.values()));
    }

    @Property
    void mapIsImmutable(@ForAll Map<String, Integer> standardMap) {
        // Convert to our Maps implementation
        List<Entry<String, Integer>> entries = new ArrayList<>(standardMap.entrySet());

        Map<String, Integer> ourMap = Maps.of(entries);

        //noinspection DataFlowIssue
        assertThrows(UnsupportedOperationException.class, () -> ourMap.put("newKey", 999));

        if (!ourMap.isEmpty()) {
            String firstKey = ourMap.keySet().iterator().next();
            //noinspection DataFlowIssue
            assertThrows(UnsupportedOperationException.class, () -> ourMap.remove(firstKey));
        }

        assertThrows(UnsupportedOperationException.class, ourMap::clear);
    }

    @Property
    void entrySetMatchesMapContents(@ForAll Map<String, Integer> standardMap) {
        // Convert to our Maps implementation
        List<Entry<String, Integer>> entries = new ArrayList<>(standardMap.entrySet());

        Map<String, Integer> ourMap = Maps.of(entries);
        Set<Entry<String, Integer>> entrySet = ourMap.entrySet();

        assertEquals(ourMap.size(), entrySet.size());

        for (Entry<String, Integer> entry : entrySet) {
            assertEquals(entry.getValue(), ourMap.get(entry.getKey()));
        }

        Map<String, Integer> reconstructed = entrySet.stream()
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        assertEquals(ourMap, reconstructed);
    }

    /**
     * Property-based tests for complex map operations and edge cases
     */

    @Property
    void mapEntriesShouldBeImmutable(@ForAll Map<String, Integer> standardMap) {
        List<Entry<String, Integer>> entries = new ArrayList<>(standardMap.entrySet());

        Map<String, Integer> ourMap = Maps.of(entries);

        // Test that entries can't be modified
        for (Entry<String, Integer> entry : ourMap.entrySet()) {
            assertThrows(UnsupportedOperationException.class, () -> entry.setValue(999));
        }
    }

    @Property
    void nonExistentKeysReturnNull(@ForAll Map<String, Integer> standardMap,
                                   @ForAll("nonExistentKey") String nonExistentKey) {
        Assume.that(!standardMap.containsKey(nonExistentKey));

        List<Entry<String, Integer>> entries = new ArrayList<>(standardMap.entrySet());

        Map<String, Integer> ourMap = Maps.of(entries);

        assertNull(ourMap.get(nonExistentKey));
        assertEquals(100, ourMap.getOrDefault(nonExistentKey, 100));
        assertFalse(ourMap.containsKey(nonExistentKey));
    }

    @Property
    void iteratingEntrySetCoversAllEntries(@ForAll Map<String, Integer> standardMap) {
        Map<String, Integer> ourMap = Maps.of(standardMap.entrySet());

        Set<String> keysFromIteration = new HashSet<>();
        Set<Integer> valuesFromIteration = new HashSet<>();

        for (Entry<String, Integer> entry : ourMap.entrySet()) {
            keysFromIteration.add(entry.getKey());
            valuesFromIteration.add(entry.getValue());
        }

        assertEquals(new HashSet<>(standardMap.keySet()), keysFromIteration);
        assertEquals(new HashSet<>(standardMap.values()), valuesFromIteration);
    }

    @Property
    void containsEntryWorksCorrectly(@ForAll Map<String, Integer> standardMap) {
        Map<String, Integer> ourMap = Maps.of(standardMap.entrySet());
        Set<Entry<String, Integer>> entrySet = ourMap.entrySet();

        // All entries from standard map should be contained in our entry set
        for (Entry<String, Integer> entry : standardMap.entrySet()) {
            assertTrue(entrySet.contains(entry));
        }

        // A modified entry should not be contained
        if (!standardMap.isEmpty()) {
            Entry<String, Integer> firstEntry = standardMap.entrySet().iterator().next();
            Entry<String, Integer> modifiedEntry = Maps.entry(
                    firstEntry.getKey(),
                    firstEntry.getValue() + 1000
            );
            assertFalse(entrySet.contains(modifiedEntry));
        }
    }

    @Property
    void forEachConsumerShouldSeeAllEntries(@ForAll Map<String, Integer> standardMap) {
        List<Entry<String, Integer>> entries = new ArrayList<>(standardMap.entrySet());

        Map<String, Integer> ourMap = Maps.of(entries);

        Map<String, Integer> collectedMap = new HashMap<>();
        //noinspection UseBulkOperation testing that forEach works correctly
        ourMap.forEach(collectedMap::put);

        assertEquals(standardMap, collectedMap);
    }

    @Provide
    Arbitrary<String> nonExistentKey() {
        return Arbitraries.strings();
    }

    @Property
    Arbitrary<Map<String, Integer>> keyValuePairs() {
        return Arbitraries.maps(Arbitraries.strings(), Arbitraries.integers());
    }


    // Additional edge case tests

    @Test
    void testHashCodeStability() {
        Map<String, Integer> map = Maps.of("a", 1, "b", 2, "c", 3);
        int initialHashCode = map.hashCode();

        // Hash code should be stable
        for (int i = 0; i < 10; i++) {
            assertEquals(initialHashCode, map.hashCode());
        }

        // Hash code should be consistent with Java's contract
        Map<String, Integer> standardMap = new HashMap<>();
        standardMap.put("a", 1);
        standardMap.put("b", 2);
        standardMap.put("c", 3);

        assertEquals(standardMap.hashCode(), map.hashCode());
    }

    @Test
    void testEqualsWithDifferentMapImplementations() {
        Map<String, Integer> mittenMap = Maps.of("a", 1, "b", 2);

        // Test equality with different Map implementations
        Map<String, Integer> hashMap = new HashMap<>();
        hashMap.put("a", 1);
        hashMap.put("b", 2);

        Map<String, Integer> linkedHashMap = new LinkedHashMap<>();
        linkedHashMap.put("a", 1);
        linkedHashMap.put("b", 2);

        Map<String, Integer> treeMap = new TreeMap<>();
        treeMap.put("a", 1);
        treeMap.put("b", 2);

        assertEquals(mittenMap, hashMap);
        assertEquals(hashMap, mittenMap);
        assertEquals(mittenMap, linkedHashMap);
        assertEquals(linkedHashMap, mittenMap);
        assertEquals(mittenMap, treeMap);
        assertEquals(treeMap, mittenMap);
    }

    @Test
    void testEqualsWithSameKeyDifferentValues() {
        Map<String, Integer> map1 = Maps.of("a", 1, "b", 2);
        Map<String, Integer> map2 = Maps.of("a", 1, "b", 3);

        assertNotEquals(map1, map2);
    }

    @Test
    void testEqualsWithDifferentKeys() {
        Map<String, Integer> map1 = Maps.of("a", 1, "b", 2);
        Map<String, Integer> map2 = Maps.of("a", 1, "c", 2);

        assertNotEquals(map1, map2);
    }

    // Stress tests for Maps implementations

    @Test
    void testLargeMapPerformance() {
        // Create a large map with 1000 entries
        @SuppressWarnings("unchecked") Entry<String, Integer>[] entries = new Entry[1000];
        for (int i = 0; i < 1000; i++) {
            entries[i] = Maps.entry("key" + i, i);
        }

        Map<String, Integer> map = Maps.of(entries);
        assertEquals(1000, map.size());

        // Test retrieval performance
        for (int i = 0; i < 1000; i++) {
            assertEquals(Integer.valueOf(i), map.get("key" + i));
        }
    }
}
