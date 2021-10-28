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
}
