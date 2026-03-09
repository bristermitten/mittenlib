package me.bristermitten.mittenlib.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StringsTest {

    @Test
    void testJoinWithCollection() {
        List<String> data = Arrays.asList("a", "b", "c");
        assertEquals("a, b, c", Strings.joinWith(data, s -> s, ", "));
    }

    @Test
    void testJoinWithCollectionIntegers() {
        List<Integer> data = Arrays.asList(1, 2, 3);
        assertEquals("1-2-3", Strings.joinWith(data, String::valueOf, "-"));
    }

    @Test
    void testJoinWithArray() {
        String[] data = {"a", "b", "c"};
        assertEquals("a, b, c", Strings.joinWith(data, s -> s, ", "));
    }

    @Test
    void testJoinWithEmptyCollection() {
        assertEquals("", Strings.joinWith(Collections.emptyList(), (Object o) -> "", ", "));
    }

    @Test
    void testJoinWithSingleElement() {
        assertEquals("a", Strings.joinWith(Collections.singletonList("a"), s -> s, ", "));
    }

    @Test
    void testJoinWithNullFunctionResultThrowsNPE() {
        List<String> data = Collections.singletonList("a");
        assertThrows(NullPointerException.class, () -> Strings.joinWith(data, s -> null, ", "));
    }
}
