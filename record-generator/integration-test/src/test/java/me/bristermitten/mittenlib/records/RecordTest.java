package me.bristermitten.mittenlib.records;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RecordTest {

    @Test
    void test() {


        TestRecord record = new TestRecord.Child2(1);
        assertEquals(TestRecord.Child2(1), record);

        record.match(() -> {
            throw new AssertionError("Should not match Child1");
        }, i -> assertEquals(1, i));

        assertEquals(1, record.matchTo(() -> -1, i -> i));

        assertTrue(record.asChild2().isPresent());
        assertFalse(record.asChild1().isPresent());
    }

    @Record
    interface TestRecordSpec {
        TestRecordSpec Child1();

        TestRecordSpec Child2(int value);
    }
}
