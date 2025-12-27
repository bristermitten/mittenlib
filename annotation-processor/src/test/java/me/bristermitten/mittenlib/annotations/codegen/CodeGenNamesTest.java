package me.bristermitten.mittenlib.annotations.codegen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CodeGenNamesTest {

    @Test
    void testVariableNames() {
        assertEquals("context", CodeGenNames.Variables.CONTEXT);
        assertEquals("dao", CodeGenNames.Variables.DAO);
        assertEquals("$data", CodeGenNames.Variables.DATA);
        assertEquals("parent", CodeGenNames.Variables.PARENT);
        assertEquals("enumValue", CodeGenNames.Variables.ENUM_VALUE);
        assertEquals("mapData", CodeGenNames.Variables.MAP_DATA);
    }

    @Test
    void testSuffixes() {
        assertEquals("FromMap", CodeGenNames.Suffixes.FROM_MAP);
    }

    @Test
    void testMethodPrefixes() {
        assertEquals("deserialize", CodeGenNames.Methods.DESERIALIZE_PREFIX);
        assertEquals("serialize", CodeGenNames.Methods.SERIALIZE_PREFIX);
    }
}
