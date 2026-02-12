package me.bristermitten.mittenlib.annotations.codegen;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VariableTest {

    @Test
    void testVariableCreation() {
        Variable var = new Variable("test", TypeName.INT);
        assertEquals("test", var.name());
        assertEquals(TypeName.INT, var.type());
    }

    @Test
    void testRef() {
        Variable var = new Variable("test", TypeName.INT);
        CodeBlock ref = var.ref();
        assertNotNull(ref);
        assertEquals("test", ref.toString());
    }

    @Test
    void testCast() {
        Variable var = new Variable("test", TypeName.INT);
        CodeBlock cast = var.cast(CodeBlock.of("value"));
        assertNotNull(cast);
        assertTrue(cast.toString().contains("(int)"));
        assertTrue(cast.toString().contains("value"));
    }

    @Test
    void testCastWithString() {
        Variable var = new Variable("test", TypeName.get(String.class));
        CodeBlock cast = var.cast("someValue");
        assertNotNull(cast);
        assertTrue(cast.toString().contains("(java.lang.String)"));
        assertTrue(cast.toString().contains("someValue"));
    }
}
