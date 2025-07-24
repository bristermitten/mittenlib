package me.bristermitten.mittenlib.codegen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RecordTest {

    @Test
    void test() {
        TestRecord r = TestRecord.create("test", 42);

        assertEquals("test", r.a());
        assertEquals(42, r.b());

        assertEquals(new TestRecord("test", 42), r);
        assertEquals("TestRecord{a=test, b=42}", r.toString());
    }

    @Record
    interface TestRecordSpec {
        @PrimaryConstructor
        TestRecordSpec create(String a, int b);


    }
}
