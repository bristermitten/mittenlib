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
}
