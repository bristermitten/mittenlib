package me.bristermitten.mittenlib.collections;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
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

    @Test
    void assertThat_set2_creationWorks() {
        final Set<String> hello = Sets.of("hello", "world");
        assertEquals(2, hello.size());
    }

    @Test
    void assertThat_set2_equalityWorks() {
        final Set<String> sets = Sets.of("hello", "world");
        final Set<String> hashSet = new HashSet<>();
        hashSet.add("hello");
        hashSet.add("world");
        assertEquals(hashSet, sets);
        assertEquals(sets, hashSet);
    }

    @Test
    void assertThat_set2_containsWorks() {
        final Set<String> sets = Sets.of("hello", "world");
        assertTrue(sets.contains("hello"));
        assertTrue(sets.contains("world"));
        assertFalse(sets.contains("hello_world"));
    }

    @Test
    void assertThat_SetConcatenation_works() {
        final Set<String> setA = Sets.of("hello");
        final Set<String> setB = Sets.of("world");
        Set<String> concat = Sets.concat(setA, setB);
        assertEquals(2, concat.size());
        assertTrue(concat.contains("hello"));
        assertTrue(concat.contains("world"));
        assertEquals(Sets.of("hello", "world"), concat);
    }

    @Test
    void assertThat_SetConcatenationTwice_works() {
        final Set<String> setA = Sets.of("hello");
        final Set<String> setB = Sets.of("world");
        Set<String> concatA = Sets.concat(setA, setB);
        Set<String> concatB = Sets.concat(concatA,  Sets.of("!"));
        assertEquals(3, concatB.size());
        assertTrue(concatB.contains("hello"));
        assertTrue(concatB.contains("world"));
        assertTrue(concatB.contains("!"));
        assertEquals(new HashSet<>(Arrays.asList("hello", "world", "!")), concatB);
    }
}
