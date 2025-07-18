package me.bristermitten.mittenlib.collections;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

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
    void assertThat_map2_containsWorks() {
        final Map<String, String> maps = Maps.of("hello", "world", "a", "b");
        assertTrue(maps.containsKey("hello"));
        assertTrue(maps.containsValue("world"));
        assertTrue(maps.containsKey("a"));
        assertTrue(maps.containsValue("b"));

        assertFalse(maps.containsKey("hello_world"));
        assertFalse(maps.containsValue("hello"));
        assertFalse(maps.containsKey("b"));
        assertFalse(maps.containsValue("a"));
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
    void assertThat_map3_equalityWorks() {
        final Map<String, String> maps = Maps.of("hello", "world", "a", "b", "c", "d");
        final Map<String, String> hashMap = new HashMap<>();
        hashMap.put("hello", "world");
        hashMap.put("a", "b");
        hashMap.put("c", "d");

        assertEquals(hashMap, maps);
        assertEquals(maps, hashMap);
    }

    @Test
    void assertThat_map3_containsWorks() {
        final Map<String, String> maps = Maps.of("hello", "world", "a", "b", "c", "d");
        assertTrue(maps.containsKey("hello"));
        assertTrue(maps.containsValue("world"));
        assertTrue(maps.containsKey("a"));
        assertTrue(maps.containsValue("b"));
        assertTrue(maps.containsKey("c"));
        assertTrue(maps.containsValue("d"));

        assertFalse(maps.containsKey("hello_world"));
        assertFalse(maps.containsValue("hello"));
        assertFalse(maps.containsKey("b"));
        assertFalse(maps.containsValue("a"));
    }

    @Test
    void assertThat_mapN_creationWorks() {
        final Map<String, String> maps = Maps.of("hello", "world", "a", "b", "c", "d", "e", "f");
        assertEquals(4, maps.size());
        assertEquals("world", maps.get("hello"));
        assertEquals("b", maps.get("a"));
        assertEquals("d", maps.get("c"));
        assertEquals("f", maps.get("e"));
    }

    @Test
    void assertThat_mapN_equalityWorks() {
        final Map<String, String> maps = Maps.of("hello", "world", "a", "b", "c", "d", "e", "f");
        final Map<String, String> hashMap = new HashMap<>();
        hashMap.put("hello", "world");
        hashMap.put("a", "b");
        hashMap.put("c", "d");
        hashMap.put("e", "f");

        assertEquals(hashMap, maps);
        assertEquals(maps, hashMap);
    }

    @Test
    void assertThat_mapN_containsWorks() {
        final Map<String, String> maps = Maps.of("hello", "world", "a", "b", "c", "d", "e", "f");
        assertTrue(maps.containsKey("hello"));
        assertTrue(maps.containsValue("world"));
        assertTrue(maps.containsKey("a"));
        assertTrue(maps.containsValue("b"));
        assertTrue(maps.containsKey("c"));
        assertTrue(maps.containsValue("d"));
        assertTrue(maps.containsKey("e"));
        assertTrue(maps.containsValue("f"));

        assertFalse(maps.containsKey("hello_world"));
        assertFalse(maps.containsValue("hello"));
        assertFalse(maps.containsKey("b"));
        assertFalse(maps.containsValue("a"));
    }

    @Test
    void assertThat_mapN_emptyWorks() {
        final Map<String, String> maps = Maps.of();
        assertTrue(maps.isEmpty());
        assertEquals(0, maps.size());
    }



}
