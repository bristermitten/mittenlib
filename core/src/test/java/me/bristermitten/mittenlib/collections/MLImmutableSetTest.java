package me.bristermitten.mittenlib.collections;

import net.jqwik.api.*;
import net.jqwik.api.constraints.Size;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the MLImmutableSet interface using both unit tests and property-based testing
 */
class MLImmutableSetTest {

    /**
     * Create a concrete MLImmutableSet for testing purposes
     */
    private static <E> MLImmutableSet<E> createTestSet(Collection<E> elements) {
        // Use the existing Sets implementation which should return MLImmutableSet instances
        Set<E> set = Sets.ofAll(elements);
        assertInstanceOf(MLImmutableSet.class, set, "Set should be an MLImmutableSet");
        return (MLImmutableSet<E>) set;
    }

    @Test
    void testImmutability() {
        MLImmutableSet<String> set = createTestSet(List.of("a", "b", "c"));

        // Test all mutating operations
        assertThrows(UnsupportedOperationException.class, () -> set.add("d"));
        assertThrows(UnsupportedOperationException.class, () -> set.remove("a"));
        assertThrows(UnsupportedOperationException.class, set::clear);
        assertThrows(UnsupportedOperationException.class, () -> set.addAll(List.of("x", "y")));
        assertThrows(UnsupportedOperationException.class, () -> set.removeAll(List.of("a", "b")));
        assertThrows(UnsupportedOperationException.class, () -> set.retainAll(List.of("a")));
        assertThrows(UnsupportedOperationException.class, () -> set.removeIf(s -> s.equals("a")));

        // Iterator should be immutable too
        Iterator<String> iterator = set.iterator();
        iterator.next();
        assertThrows(UnsupportedOperationException.class, iterator::remove);
    }

    @Test
    void testPlusOperation() {
        MLImmutableSet<String> set = createTestSet(List.of("a", "b", "c"));

        // Test plus operation
        MLImmutableSet<String> newSet = set.plus("d");

        // Original set should remain unchanged
        assertEquals(3, set.size());
        assertTrue(set.containsAll(List.of("a", "b", "c")));
        assertFalse(set.contains("d"));

        // New set should have the added element
        assertEquals(4, newSet.size());
        assertTrue(newSet.containsAll(List.of("a", "b", "c", "d")));
    }

    @Test
    void testPlusWithExistingElement() {
        MLImmutableSet<String> set = createTestSet(List.of("a", "b", "c"));

        // Adding existing element should return equivalent set
        MLImmutableSet<String> newSet = set.plus("a");

        assertEquals(3, newSet.size());
        assertTrue(newSet.containsAll(List.of("a", "b", "c")));
        assertEquals(set, newSet);
    }

    @Test
    void testPlusWithNull() {
        MLImmutableSet<String> set = createTestSet(List.of("a", "b", "c"));

        // Adding null should throw
        assertThrows(NullPointerException.class, () -> set.plus(null));
    }

    @Test
    void testEmptySetPlusOperation() {
        MLImmutableSet<String> emptySet = createTestSet(Collections.emptyList());

        MLImmutableSet<String> newSet = emptySet.plus("a");

        assertEquals(0, emptySet.size());
        assertEquals(1, newSet.size());
        assertTrue(newSet.contains("a"));
    }

    // Property-based tests

    @Property
    void plusPreservesExistingElements(@ForAll @Size(max = 10) List<String> elements,
                                       @ForAll("nonNullString") String newElement) {
        Assume.that(elements.stream().allMatch(Objects::nonNull));

        MLImmutableSet<String> set = createTestSet(elements);
        MLImmutableSet<String> newSet = set.plus(newElement);

        // All original elements should still be present
        for (String element : set) {
            assertTrue(newSet.contains(element));
        }

        // New element should be present
        assertTrue(newSet.contains(newElement));

        // Size should be original size + 1 only if newElement wasn't already in the set
        if (set.contains(newElement)) {
            assertEquals(set.size(), newSet.size());
        } else {
            assertEquals(set.size() + 1, newSet.size());
        }
    }

    @Property
    void plusOperationIsIdempotent(@ForAll @Size(max = 10) List<String> elements,
                                   @ForAll("nonNullString") String newElement) {
        Assume.that(elements.stream().allMatch(Objects::nonNull));

        MLImmutableSet<String> set = createTestSet(elements);
        MLImmutableSet<String> newSet1 = set.plus(newElement);
        MLImmutableSet<String> newSet2 = newSet1.plus(newElement);

        // Adding the same element twice should be the same as adding it once
        assertEquals(newSet1, newSet2);
        assertEquals(newSet1.size(), newSet2.size());
    }

    @Property
    void originalSetRemainsUnchanged(@ForAll @Size(max = 10) List<String> elements,
                                     @ForAll("nonNullString") String newElement) {
        Assume.that(elements.stream().allMatch(Objects::nonNull));

        MLImmutableSet<String> originalSet = createTestSet(elements);
        Set<String> expectedSet = new HashSet<>(elements);

        // Create a copy of the original set for comparison
        Set<String> originalElementsCopy = new HashSet<>(originalSet);

        // Perform the operation
        originalSet.plus(newElement);

        // Original set should remain unchanged
        assertEquals(originalElementsCopy, originalSet);
        assertEquals(expectedSet.size(), originalSet.size());

        for (String element : expectedSet) {
            assertTrue(originalSet.contains(element));
        }
    }

    @Provide
    Arbitrary<String> nonNullString() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10);
    }
}
