package me.bristermitten.mittenlib.codegen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UnionTest {

    @Test
    void test() {
        TestUnion record = new TestUnion.Child2(1);
        assertEquals(TestUnion.Child2(1), record);

        record.match(() -> {
            throw new AssertionError("Should not match Child1");
        }, i -> assertEquals(1, i));

        assertEquals(1, record.matchTo(() -> -1, i -> i));

        assertTrue(record.asChild2().isPresent());
        assertFalse(record.asChild1().isPresent());
    }

    @Union
    interface TestUnionSpec {
        TestUnionSpec Child1();

        TestUnionSpec Child2(int value);
    }
}
