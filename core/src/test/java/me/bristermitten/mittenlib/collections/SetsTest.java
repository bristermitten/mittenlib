package me.bristermitten.mittenlib.collections;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SetsTest {

    @Test
    void assertThat_singletonSet_creationWorks() {
        final Set<String> hello = Sets.of("hello");
        assertEquals(1, hello.size());
    }

    @Test
    void assertThat_singletonSet_equalityWorks() {
        final Set<String> sets = Sets.of("hello");
        final Set<String> hashSet = new HashSet<>();
        hashSet.add("hello");
        assertEquals(hashSet, sets);
        assertEquals(sets, hashSet);
    }

    @Test
    void assertThat_singletonSet_containsWorks() {
        final Set<String> sets = Sets.of("hello");
        assertTrue(sets.contains("hello"));
        assertFalse(sets.contains("hello_world"));
    }
}
