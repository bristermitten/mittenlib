package me.bristermitten.mittenlib.annotations.codegen;

import com.squareup.javapoet.TypeName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScopeTest {

    @Test
    void testDeclareVariable() {
        Scope scope = new Scope();
        Variable var = scope.declare("test", TypeName.INT);
        
        assertEquals("test", var.name());
        assertEquals(TypeName.INT, var.type());
        assertTrue(scope.contains("test"));
    }

    @Test
    void testDeclareAnonymousVariable() {
        Scope scope = new Scope();
        Variable var1 = scope.declareAnonymous(TypeName.INT);
        Variable var2 = scope.declareAnonymous(TypeName.get(String.class));
        
        assertEquals("var0", var1.name());
        assertEquals("var1", var2.name());
        assertTrue(scope.contains("var0"));
        assertTrue(scope.contains("var1"));
    }

    @Test
    void testDuplicateDeclarationThrows() {
        Scope scope = new Scope();
        scope.declare("test", TypeName.INT);
        
        assertThrows(IllegalStateException.class, () -> {
            scope.declare("test", TypeName.get(String.class));
        });
    }

    @Test
    void testGetVariable() {
        Scope scope = new Scope();
        Variable var = scope.declare("test", TypeName.INT);
        
        assertTrue(scope.get("test").isPresent());
        assertEquals(var, scope.get("test").get());
        assertFalse(scope.get("nonexistent").isPresent());
    }

    @Test
    void testAllVariables() {
        Scope scope = new Scope();
        Variable var1 = scope.declare("first", TypeName.INT);
        Variable var2 = scope.declare("second", TypeName.get(String.class));
        
        List<Variable> allVars = scope.allVariables();
        assertEquals(2, allVars.size());
        assertEquals(var1, allVars.get(0));
        assertEquals(var2, allVars.get(1));
    }

    @Test
    void testCreateChild() {
        Scope parent = new Scope();
        Variable parentVar = parent.declare("parent", TypeName.INT);
        parent.declareAnonymous(TypeName.get(String.class)); // var0
        
        Scope child = parent.createChild();
        assertTrue(child.contains("parent"));
        assertTrue(child.contains("var0"));
        
        Variable childVar = child.declare("child", TypeName.BOOLEAN);
        assertTrue(child.contains("child"));
        assertFalse(parent.contains("child"));
        
        // Child counter continues from parent
        Variable childAnon = child.declareAnonymous(TypeName.DOUBLE);
        assertEquals("var1", childAnon.name());
    }
}
