package me.bristermitten.mittenlib.codegen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UnionSpecTest {

    @Test
    void testStructural() {
        TestStructuralUnion record = new TestStructuralUnion.Child2(1);
        assertEquals(TestStructuralUnion.Child2(1), record);

        record.match(() -> {
            throw new AssertionError("Should not match Child1");
        }, i -> assertEquals(1, i));

        assertEquals(1, record.matchTo(() -> -1, i -> i));

        assertTrue(record.asChild2().isPresent());
        assertFalse(record.asChild1().isPresent());
    }

    @Test
    void testNominal() {
        TestNominalUnion record = new TestNominalUnion.Child2(1);
        assertEquals(new TestNominalUnion.Child2(1), record);

        record.match(child1 -> {
            throw new AssertionError("Should not match Child1");
        }, child2 -> assertEquals(1, child2.value()));

        assertEquals(1, (int) record.matchTo(child1 -> -1, TestNominalUnion.Child2::value));

        assertTrue(record.asChild2().isPresent());
        assertFalse(record.asChild1().isPresent());

    }

    @SuppressWarnings("unused")
    @UnionSpec
    @MatchStrategy(MatchStrategies.STRUCTURAL)
    interface TestStructuralUnionSpec {
        TestStructuralUnionSpec Child1();

        TestStructuralUnionSpec Child2(int value);
    }

    @SuppressWarnings("unused")
    @UnionSpec
    @MatchStrategy(MatchStrategies.NOMINAL)
    interface TestNominalUnionSpec {
        TestNominalUnionSpec Child1();

        TestNominalUnionSpec Child2(int value);
    }
}
