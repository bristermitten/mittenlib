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
    void assertThat_set2_respectsEquality() {
        final Set<String> hello = Sets.of("hello", "hello");
        assertEquals(1, hello.size());
        assertEquals(Sets.of("hello"), hello);
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
    void assertThat_SetUnion_works() {
        final Set<String> setA = Sets.of("hello");
        final Set<String> setB = Sets.of("world");
        Set<String> union = Sets.union(setA, setB);
        assertEquals(2, union.size());
        assertTrue(union.contains("hello"));
        assertTrue(union.contains("world"));
        assertEquals(Sets.of("hello", "world"), union);
    }

    @Test
    void assertThat_SetUnionTwice_works() {
        final Set<String> setA = Sets.of("hello");
        final Set<String> setB = Sets.of("world");
        Set<String> unionA = Sets.union(setA, setB);
        Set<String> unionB = Sets.union(unionA, Sets.of("!"));
        assertEquals(3, unionB.size());
        assertTrue(unionB.contains("hello"));
        assertTrue(unionB.contains("world"));
        assertTrue(unionB.contains("!"));
        assertEquals(new HashSet<>(Arrays.asList("hello", "world", "!")), unionB);
    }

    @Test
    void assertThat_SetDifference_works() {
        final Set<String> setA = Sets.of("hello", "world");
        final Set<String> setB = Sets.of("world");
        Set<String> difference = Sets.difference(setA, setB);
        assertEquals(1, difference.size());
        assertTrue(difference.contains("hello"));
        assertFalse(difference.contains("world"));
        assertEquals(Sets.of("hello"), difference);
    }

    @Test
    void assertThat_SetDifference_isIdempotent() {
        final Set<String> setA = Sets.of("hello", "world");
        final Set<String> setB = Sets.of("world");
        Set<String> difference = Sets.difference(Sets.difference(setA, setB), setB);
        assertEquals(1, difference.size());
        assertTrue(difference.contains("hello"));
        assertFalse(difference.contains("world"));
        assertEquals(Sets.of("hello"), difference);
    }
}
