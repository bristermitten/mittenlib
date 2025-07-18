package me.bristermitten.mittenlib.collections;

import net.jqwik.api.*;
import net.jqwik.api.constraints.Size;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for {@link Sets} using both unit tests and property-based testing
 */
class SetsTest {

    @Test
    void testEmptySet() {
        Set<String> set = Sets.of();

        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
        assertFalse(set.contains("anything"));
        assertEquals(Collections.emptySet(), set);
    }

    @Test
    void testSingleElementSet() {
        Set<String> set = Sets.of("element");

        assertEquals(1, set.size());
        assertTrue(set.contains("element"));
        assertFalse(set.contains("other"));
        assertEquals(Set.of("element"), set);
    }

    @Test
    void testTwoElementSet() {
        Set<String> set = Sets.of("a", "b");

        assertEquals(2, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertFalse(set.contains("c"));
        assertEquals(Set.of("a", "b"), set);
    }

    @Test
    void testTwoElementSetWithDuplicates() {
        Set<String> set = Sets.of("a", "a");

        assertEquals(1, set.size());
        assertTrue(set.contains("a"));
        assertEquals(Set.of("a"), set);
    }

    @Test
    void testThreeElementSet() {
        Set<String> set = Sets.of("a", "b", "c");

        assertEquals(3, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains("c"));
        assertEquals(Set.of("a", "b", "c"), set);
    }

    @Test
    void testThreeElementSetWithDuplicates() {
        // Test various duplicate scenarios
        assertEquals(1, Sets.of("a", "a", "a").size());
        assertEquals(2, Sets.of("a", "a", "b").size());
        assertEquals(2, Sets.of("a", "b", "a").size());
        assertEquals(2, Sets.of("a", "b", "b").size());

        assertTrue(Sets.of("a", "a", "b").containsAll(Set.of("a", "b")));
    }

    @Test
    void testVarArgsSet() {
        Set<String> set = Sets.of("a", "b", "c", "d", "e");

        assertEquals(5, set.size());
        for (String element : Arrays.asList("a", "b", "c", "d", "e")) {
            assertTrue(set.contains(element));
        }
    }

    @Test
    void testVarArgsSetWithDuplicates() {
        Set<String> set = Sets.of("a", "b", "a", "c", "b", "d");

        assertEquals(4, set.size());
        assertEquals(Set.of("a", "b", "c", "d"), set);
    }

    @Test
    void testSetFromCollection() {
        List<String> list = Arrays.asList("a", "b", "c", "b");
        Set<String> set = Sets.ofAll(list);

        assertEquals(3, set.size());
        assertEquals(Set.of("a", "b", "c"), set);
    }

    @Test
    void testSetFromEmptyCollection() {
        Set<String> set = Sets.ofAll(Collections.emptyList());

        assertTrue(set.isEmpty());
        assertEquals(Sets.of(), set);
    }

    @Test
    void testUnionOperation() {
        Set<String> set1 = Sets.of("a", "b");
        Set<String> set2 = Sets.of("b", "c");
        Set<String> union = Sets.union(set1, set2);

        assertEquals(3, union.size());
        assertEquals(Set.of("a", "b", "c"), union);
    }

    @Test
    void testUnionWithEmptySets() {
        Set<String> empty1 = Sets.of();
        Set<String> empty2 = Sets.of();
        Set<String> nonEmpty = Sets.of("a", "b");

        assertEquals(Sets.of(), Sets.union(empty1, empty2));
        assertEquals(nonEmpty, Sets.union(empty1, nonEmpty));
        assertEquals(nonEmpty, Sets.union(nonEmpty, empty1));
    }

    @Test
    void testImmutabilityOperations() {
        Set<String> set = Sets.of("a", "b", "c");

        assertThrows(UnsupportedOperationException.class, () -> set.add("d"));
        assertThrows(UnsupportedOperationException.class, () -> set.remove("a"));
        assertThrows(UnsupportedOperationException.class, set::clear);
        assertThrows(UnsupportedOperationException.class, () -> set.addAll(List.of("x", "y")));
        assertThrows(UnsupportedOperationException.class, () -> set.removeAll(List.of("a", "b")));
        assertThrows(UnsupportedOperationException.class, () -> set.retainAll(List.of("a")));
        assertThrows(UnsupportedOperationException.class, () -> set.removeIf(s -> s.equals("a")));
    }

    @Test
    void testIteratorImmutability() {
        Set<String> set = Sets.of("a", "b", "c");
        Iterator<String> iterator = set.iterator();

        assertTrue(iterator.hasNext());
        iterator.next();
        assertThrows(UnsupportedOperationException.class, iterator::remove);
    }

    @Test
    void testNullElementsThrow() {
        assertThrows(NullPointerException.class, () -> Sets.of((String) null));
        assertThrows(NullPointerException.class, () -> Sets.of("a", null));
        assertThrows(NullPointerException.class, () -> Sets.of("a", "b", null));
        assertThrows(NullPointerException.class, () -> Sets.of("a", null, "b", "c"));
    }

    @Test
    void testSetEquality() {
        Set<String> set1 = Sets.of("a", "b", "c");
        Set<String> set2 = Sets.of("c", "a", "b");
        Set<String> standardSet = Set.of("a", "b", "c");

        assertEquals(set1, set2);
        assertEquals(set1, standardSet);
        assertEquals(set1.hashCode(), set2.hashCode());
        assertEquals(set1.hashCode(), standardSet.hashCode());
    }

    // Property-based tests using jqwik

    @Property
    void setContainsAllAddedElements(@ForAll @Size(min = 1, max = 10) List<String> elements) {
        Assume.that(elements.stream().allMatch(Objects::nonNull));

        Set<String> set = Sets.of(elements.toArray(new String[0]));
        Set<String> distinctElements = new HashSet<>(elements);

        assertEquals(distinctElements.size(), set.size());

        for (String element : distinctElements) {
            assertTrue(set.contains(element));
        }
    }

    @Property
    void setIsImmutable(@ForAll @Size(min = 1, max = 5) List<String> elements) {
        Assume.that(elements.stream().allMatch(Objects::nonNull));

        Set<String> set = Sets.ofAll(elements);

        assertThrows(UnsupportedOperationException.class, () -> set.add("new"));
        if (!set.isEmpty()) {
            String firstElement = set.iterator().next();
            assertThrows(UnsupportedOperationException.class, () -> set.remove(firstElement));
        }
        assertThrows(UnsupportedOperationException.class, set::clear);
    }

    @Property
    void setEqualityIsSymmetric(@ForAll @Size(min = 1, max = 5) List<String> elements) {
        Assume.that(elements.stream().allMatch(Objects::nonNull));

        Set<String> set1 = Sets.of(elements.toArray(new String[0]));
        List<String> shuffled = new ArrayList<>(elements);
        Collections.shuffle(shuffled);
        Set<String> set2 = Sets.of(shuffled.toArray(new String[0]));

        assertEquals(set1, set2);
        assertEquals(set2, set1);
        assertEquals(set1.hashCode(), set2.hashCode());
    }

    @Property
    void setBehavesLikeStandardSet(@ForAll @Size(min = 1, max = 5) List<String> elements) {
        Assume.that(elements.stream().allMatch(Objects::nonNull));

        Set<String> customSet = Sets.of(elements.toArray(new String[0]));
        Set<String> standardSet = new HashSet<>(elements);

        assertEquals(standardSet.size(), customSet.size());
        assertEquals(standardSet, customSet);

        for (String element : standardSet) {
            assertEquals(standardSet.contains(element), customSet.contains(element));
        }
    }

    @Property
    void unionIsCommutative(@ForAll("nonNullStringLists") List<String> elements1,
                            @ForAll("nonNullStringLists") List<String> elements2) {

        Set<String> set1 = Sets.of(elements1.toArray(new String[0]));
        Set<String> set2 = Sets.of(elements2.toArray(new String[0]));

        Set<String> union1 = Sets.union(set1, set2);
        Set<String> union2 = Sets.union(set2, set1);

        assertEquals(union1, union2);
    }

    @Property
    void unionContainsAllElements(@ForAll("nonNullStringLists") List<String> elements1,
                                  @ForAll("nonNullStringLists") List<String> elements2) {

        Set<String> set1 = Sets.of(elements1.toArray(new String[0]));
        Set<String> set2 = Sets.of(elements2.toArray(new String[0]));
        Set<String> union = Sets.union(set1, set2);

        for (String element : set1) {
            assertTrue(union.contains(element));
        }
        for (String element : set2) {
            assertTrue(union.contains(element));
        }

        // Union size should be <= sum of individual set sizes (due to overlap)
        assertTrue(union.size() <= set1.size() + set2.size());
    }

    @Provide
    Arbitrary<List<String>> nonNullStringLists() {
        return Arbitraries.strings().alpha().ofMaxLength(10)
                .list().ofMaxSize(5);
    }

    @Property
    void ofAllPreservesUniqueness(@ForAll @Size(min = 1, max = 10) List<String> elements) {
        Assume.that(elements.stream().allMatch(Objects::nonNull));

        Set<String> set = Sets.ofAll(elements);
        Set<String> expectedSet = new HashSet<>(elements);

        assertEquals(expectedSet.size(), set.size());
        assertEquals(expectedSet, set);
    }

    @Property
    void duplicateElementsAreHandledCorrectly(@ForAll("nonNullString") String element, @ForAll("smallPositiveInt") int count) {
        String[] duplicates = new String[count];
        Arrays.fill(duplicates, element);

        Set<String> set = Sets.of(duplicates);

        assertEquals(1, set.size());
        assertTrue(set.contains(element));
        assertEquals(Set.of(element), set);
    }

    @Provide
    Arbitrary<String> nonNullString() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10);
    }

    @Provide
    Arbitrary<Integer> smallPositiveInt() {
        return Arbitraries.integers().between(2, 5);
    }

    // Stress tests for edge cases

    @Test
    void testLargeSetCreation() {
        String[] elements = new String[1000];
        for (int i = 0; i < 1000; i++) {
            elements[i] = "element" + i;
        }

        Set<String> set = Sets.of(elements);
        assertEquals(1000, set.size());

        for (int i = 0; i < 1000; i++) {
            assertTrue(set.contains("element" + i));
        }
    }

    @Test
    void testLargeSetWithDuplicates() {
        String[] elements = new String[1000];
        for (int i = 0; i < 1000; i++) {
            elements[i] = "element" + (i % 100); // Only 100 unique elements
        }

        Set<String> set = Sets.of(elements);
        assertEquals(100, set.size());

        for (int i = 0; i < 100; i++) {
            assertTrue(set.contains("element" + i));
        }
    }

    @Test
    void testSetWithComplexObjects() {
        List<String> list1 = List.of("a", "b");
        List<String> list2 = List.of("c", "d");
        List<String> list3 = List.of("a", "b"); // Same as list1

        Set<List<String>> set = Sets.of(list1, list2, list3);

        assertEquals(2, set.size()); // list1 and list3 are equal, so only 2 unique elements
        assertTrue(set.contains(list1));
        assertTrue(set.contains(list2));
        assertTrue(set.contains(list3)); // Should be true since list3.equals(list1)
    }

    @Test
    void testSetIterationOrder() {
        // While we don't guarantee order, we should be able to iterate through all elements
        Set<String> set = Sets.of("a", "b", "c", "d", "e");

        Set<String> iteratedElements = new HashSet<>();
        for (String element : set) {
            iteratedElements.add(element);
        }

        assertEquals(set, iteratedElements);
    }

    @Test
    void testOfAllWithExistingMittenLibSet() {
        Set<String> originalSet = Sets.of("a", "b", "c");
        Set<String> newSet = Sets.ofAll(originalSet);

        // Should return the same instance or an equivalent one
        assertEquals(originalSet, newSet);
    }

    /**
     * Property-based tests for complex set operations and edge cases
     */

    @Property
    void setContainsNoMoreThanUniqueElements(@ForAll @Size(min = 1, max = 20) List<String> elements) {
        Assume.that(elements.stream().allMatch(Objects::nonNull));

        Set<String> uniqueElements = new HashSet<>(elements);
        Set<String> set = Sets.of(elements.toArray(new String[0]));

        // Size should match unique elements count
        assertEquals(uniqueElements.size(), set.size());

        // All unique elements should be in the set
        for (String element : uniqueElements) {
            assertTrue(set.contains(element));
        }
    }

    @Property
    void setUnionSizeIsCorrect(@ForAll("nonNullStringLists") List<String> list1,
                               @ForAll("nonNullStringLists") List<String> list2) {

        Set<String> set1 = Sets.ofAll(list1);
        Set<String> set2 = Sets.ofAll(list2);
        Set<String> union = Sets.union(set1, set2);

        // Calculate the expected size mathematically
        Set<String> expectedUnion = new HashSet<>(list1);
        expectedUnion.addAll(list2);

        assertEquals(expectedUnion.size(), union.size());
        assertEquals(expectedUnion, union);
    }

    @Property
    void unionIsAssociative(@ForAll("nonNullStringLists") List<String> list1,
                            @ForAll("nonNullStringLists") List<String> list2,
                            @ForAll("nonNullStringLists") List<String> list3) {

        Set<String> set1 = Sets.of(list1.toArray(new String[0]));
        Set<String> set2 = Sets.of(list2.toArray(new String[0]));
        Set<String> set3 = Sets.of(list3.toArray(new String[0]));

        // (A ∪ B) ∪ C = A ∪ (B ∪ C)
        Set<String> leftSide = Sets.union(Sets.union(set1, set2), set3);
        Set<String> rightSide = Sets.union(set1, Sets.union(set2, set3));

        assertEquals(leftSide, rightSide);
    }

    @Property
    void setWithNullElementsThrows(@ForAll @Size(min = 1, max = 10) List<String> validElements) {
        Assume.that(validElements.stream().allMatch(Objects::nonNull));

        // Test with one null at different positions
        for (int i = 0; i <= validElements.size(); i++) {
            List<String> withNull = new ArrayList<>(validElements);
            withNull.add(i, null);

            final int position = i; // Effectively final for lambda
            assertThrows(NullPointerException.class, () ->
                    Sets.of(withNull.toArray(new String[0])));
        }
    }

    @Property
    void hashCodeEqualsForSameSets(@ForAll("nonNullStringLists") List<String> elements) {
        // Create two sets with the same elements but potentially different order
        List<String> shuffled = new ArrayList<>(elements);
        Collections.shuffle(shuffled);

        Set<String> set1 = Sets.of(elements.toArray(new String[0]));
        Set<String> set2 = Sets.of(shuffled.toArray(new String[0]));

        assertEquals(set1, set2);
        assertEquals(set1.hashCode(), set2.hashCode());
    }

    @Property
    void addingElementsToSetViaStreamProducesCorrectResult(@ForAll("nonNullStringLists") List<String> elements) {
        // Get unique elements via stream
        Set<String> expectedSet = elements.stream().collect(Collectors.toSet());

        // Use our Sets.of implementation
        Set<String> actualSet = Sets.of(elements.toArray(new String[0]));

        assertEquals(expectedSet.size(), actualSet.size());
        assertEquals(expectedSet, actualSet);
    }

    @Property
    void setContainsElementsViaContainsAll(@ForAll("nonNullStringLists") List<String> elements) {
        Set<String> set = Sets.of(elements.toArray(new String[0]));
        Set<String> uniqueElements = new HashSet<>(elements);

        // Test containsAll
        assertTrue(set.containsAll(uniqueElements));
        assertTrue(uniqueElements.containsAll(set));
    }

    // Additional edge case tests

    @Test
    void testHashCodeStability() {
        Set<String> set = Sets.of("a", "b", "c");
        int initialHashCode = set.hashCode();

        // Hash code should be stable
        for (int i = 0; i < 10; i++) {
            assertEquals(initialHashCode, set.hashCode());
        }

        // Hash code should be consistent with Java's contract
        Set<String> standardSet = new HashSet<>(Set.of("a", "b", "c"));

        assertEquals(standardSet.hashCode(), set.hashCode());
    }

    @Test
    void testEqualsWithDifferentSetImplementations() {
        Set<String> mittenSet = Sets.of("a", "b", "c");

        // Test equality with different Set implementations
        Set<String> hashSet = new HashSet<>();
        hashSet.add("a");
        hashSet.add("b");
        hashSet.add("c");

        Set<String> linkedHashSet = new LinkedHashSet<>();
        linkedHashSet.add("a");
        linkedHashSet.add("b");
        linkedHashSet.add("c");

        Set<String> treeSet = new TreeSet<>();
        treeSet.add("a");
        treeSet.add("b");
        treeSet.add("c");

        assertEquals(mittenSet, hashSet);
        assertEquals(hashSet, mittenSet);
        assertEquals(mittenSet, linkedHashSet);
        assertEquals(linkedHashSet, mittenSet);
        assertEquals(mittenSet, treeSet);
        assertEquals(treeSet, mittenSet);
    }

    @Test
    void testUnionWithSharedReferences() {
        // Create sets with shared references
        List<String> sharedObj1 = List.of("shared1");
        List<String> sharedObj2 = List.of("shared2");

        Set<List<String>> set1 = Sets.of(sharedObj1, sharedObj2);
        Set<List<String>> set2 = Sets.of(sharedObj2, List.of("unique"));

        Set<List<String>> union = Sets.union(set1, set2);

        assertEquals(3, union.size());
        assertTrue(union.contains(sharedObj1));
        assertTrue(union.contains(sharedObj2));
        assertTrue(union.contains(List.of("unique")));
    }

    @Test
    void testToArray() {
        Set<String> set = Sets.of("a", "b", "c");

        // Test toArray() without argument
        Object[] array = set.toArray();
        assertEquals(3, array.length);

        Set<String> reconstructed = new HashSet<>();
        for (Object obj : array) {
            assertInstanceOf(String.class, obj);
            reconstructed.add((String) obj);
        }
        assertEquals(set, reconstructed);

        // Test toArray(T[]) with exact size
        String[] strArray = set.toArray(new String[3]);
        assertEquals(3, strArray.length);
        assertEquals(Set.of("a", "b", "c"), Set.of(strArray));

        // Test toArray(T[]) with larger array
        String[] largerArray = set.toArray(new String[5]);
        assertEquals(5, largerArray.length);
        assertNull(largerArray[3]); // Extra elements should be null
        assertNull(largerArray[4]);

        // Test toArray(T[]) with smaller array
        String[] smallerArray = set.toArray(new String[1]);
        assertEquals(3, smallerArray.length); // Should resize
        assertEquals(Set.of("a", "b", "c"), Set.of(smallerArray));
    }

    @Test
    void testPerformanceWithLargeNumberOfUniques() {
        // Create set with large number of unique elements
        String[] elements = new String[10000];
        for (int i = 0; i < elements.length; i++) {
            elements[i] = "element" + i;
        }

        Set<String> set = Sets.of(elements);
        assertEquals(10000, set.size());

        // Check contains performance
        for (int i = 0; i < 100; i++) {
            int index = i * 100; // Sample every 100th element
            assertTrue(set.contains("element" + index));
        }

        // Check non-existing elements
        for (int i = 0; i < 100; i++) {
            assertFalse(set.contains("missing" + i));
        }
    }
}
