package me.bristermitten.mittenlib.annotations.codegen;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CodeGenBuilderTest {

    @Test
    void testAddStatement() {
        CodeGenBuilder builder = new CodeGenBuilder();
        builder.addStatement("int x = $L", 5);
        
        CodeBlock block = builder.build();
        assertNotNull(block);
        assertTrue(block.toString().contains("int x = 5"));
    }

    @Test
    void testControlFlow() {
        CodeGenBuilder builder = new CodeGenBuilder();
        builder.beginControlFlow("if (x > 0)");
        assertEquals(1, builder.indentLevel());
        builder.addStatement("System.out.println($S)", "positive");
        builder.endControlFlow();
        assertEquals(0, builder.indentLevel());
        
        CodeBlock block = builder.build();
        assertNotNull(block);
    }

    @Test
    void testUnclosedControlFlowThrows() {
        CodeGenBuilder builder = new CodeGenBuilder();
        builder.beginControlFlow("if (x > 0)");
        
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void testDeclareVariable() {
        CodeGenBuilder builder = new CodeGenBuilder();
        Variable var = builder.declareVariable("test", TypeName.INT, CodeBlock.of("5"));
        
        assertEquals("test", var.name());
        assertEquals(TypeName.INT, var.type());
        assertTrue(builder.scope().contains("test"));
        
        CodeBlock block = builder.build();
        assertTrue(block.toString().contains("int test = 5"));
    }

    @Test
    void testDeclareAnonymous() {
        CodeGenBuilder builder = new CodeGenBuilder();
        Variable var = builder.declareAnonymous(TypeName.INT, CodeBlock.of("10"));
        
        assertEquals("var0", var.name());
        assertTrue(builder.scope().contains("var0"));
        
        CodeBlock block = builder.build();
        assertTrue(block.toString().contains("int var0 = 10"));
    }

    @Test
    void testIndentTracking() {
        CodeGenBuilder builder = new CodeGenBuilder();
        assertEquals(0, builder.indentLevel());
        
        builder.indent();
        assertEquals(1, builder.indentLevel());
        
        builder.indent();
        assertEquals(2, builder.indentLevel());
        
        builder.unindent();
        assertEquals(1, builder.indentLevel());
        
        builder.unindent();
        assertEquals(0, builder.indentLevel());
    }

    @Test
    void testUnindentBelowZeroThrows() {
        CodeGenBuilder builder = new CodeGenBuilder();
        assertThrows(IllegalStateException.class, builder::unindent);
    }

    @Test
    void testEndControlFlowWithoutBeginThrows() {
        CodeGenBuilder builder = new CodeGenBuilder();
        assertThrows(IllegalStateException.class, builder::endControlFlow);
    }
}
