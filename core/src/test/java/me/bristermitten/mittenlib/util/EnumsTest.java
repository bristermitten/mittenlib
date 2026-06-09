package me.bristermitten.mittenlib.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EnumsTest {

    @Test
    void prettyName() {
        assertEquals("Entry A", Enums.prettyName(TestEnum.ENTRY_A));
        assertEquals("Entry B", Enums.prettyName(TestEnum.ENTRY_B));
        assertEquals("Entry C", Enums.prettyName(TestEnum.ENTRY_C));
        assertEquals("Entryd", Enums.prettyName(TestEnum.ENTRYD));
    }

    private enum TestEnum {
        ENTRY_A,
        ENTRY_B,
        ENTRY_C,
        ENTRYD
    }
}
