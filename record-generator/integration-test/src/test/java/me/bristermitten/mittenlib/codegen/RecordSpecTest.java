package me.bristermitten.mittenlib.codegen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RecordSpecTest {

    @Test
    void test() {
        TestRecord r = new TestRecord("test", 42);

        assertEquals("test", r.a());
        assertEquals(42, r.b());

        assertEquals(new TestRecord("test", 42), r);
        assertEquals("TestRecord{a=test, b=42}", r.toString());

        TestRecord newRecord = r.withA("newTest");
        assertEquals("newTest", newRecord.a());
        assertEquals(42, newRecord.b());
        assertEquals("TestRecord{a=newTest, b=42}", newRecord.toString());
        assertEquals(new TestRecord("newTest", 42), newRecord);
    }

    @RecordSpec
    interface TestRecordSpec {
        String a();

        int b();
    }
}
