package me.bristermitten.mittenlib.annotations.codegen;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlatMapChainBuilderTest {

    @Test
    void testSingleOperation() {
        FlatMapChainBuilder chain = new FlatMapChainBuilder();
        chain.addOperation("deserializeX(context)", TypeName.get(String.class));
        
        CodeBlock result = chain.buildWithConstructor(
            me.bristermitten.mittenlib.util.Result.class,
            ClassName.get("com.example", "Config")
        );
        
        String code = result.toString();
        assertTrue(code.contains("return"));
        assertTrue(code.contains("deserializeX(context)"));
        assertTrue(code.contains("flatMap(var0 ->"));
        assertTrue(code.contains("Result.ok(new Config(var0))"));
    }

    @Test
    void testMultipleOperations() {
        FlatMapChainBuilder chain = new FlatMapChainBuilder();
        chain.addOperation("deserializeX(context)", TypeName.get(String.class));
        chain.addOperation("deserializeY(context)", TypeName.INT);
        chain.addOperation("deserializeZ(context)", TypeName.BOOLEAN);
        
        CodeBlock result = chain.buildWithConstructor(
            me.bristermitten.mittenlib.util.Result.class,
            ClassName.get("com.example", "Config")
        );
        
        String code = result.toString();
        assertTrue(code.contains("flatMap(var0 ->"));
        assertTrue(code.contains("flatMap(var1 ->"));
        assertTrue(code.contains("flatMap(var2 ->"));
        assertTrue(code.contains("Config(var0, var1, var2)"));
        // Should have 3 closing parentheses for flatMaps
        long closingParens = code.chars().filter(ch -> ch == ')').count();
        // At least 3 for flatMaps + 2 for ok() and new Config()
        assertTrue(closingParens >= 5);
    }

    @Test
    void testEmptyChainThrows() {
        FlatMapChainBuilder chain = new FlatMapChainBuilder();
        
        assertThrows(IllegalStateException.class, () -> {
            chain.buildWithConstructor(
                me.bristermitten.mittenlib.util.Result.class,
                ClassName.get("com.example", "Config")
            );
        });
    }

    @Test
    void testGetVariables() {
        FlatMapChainBuilder chain = new FlatMapChainBuilder();
        chain.addOperation("op1()", TypeName.get(String.class));
        chain.addOperation("op2()", TypeName.INT);
        
        var variables = chain.getVariables();
        assertEquals(2, variables.size());
        assertEquals("var0", variables.get(0).name());
        assertEquals("var1", variables.get(1).name());
        assertEquals(TypeName.get(String.class), variables.get(0).type());
        assertEquals(TypeName.INT, variables.get(1).type());
    }

    @Test
    void testScopeInheritance() {
        Scope parentScope = new Scope();
        parentScope.declare("existing", TypeName.get(String.class));
        
        FlatMapChainBuilder chain = new FlatMapChainBuilder(parentScope);
        chain.addOperation("op1()", TypeName.INT);
        
        // Variables from chain should start after existing ones
        var variables = chain.getVariables();
        assertEquals(1, variables.size());
        // Should inherit counter from parent but this is an anonymous var
        assertTrue(chain.getScope().contains("existing"));
    }

    @Test
    void testAddOperationWithFormat() {
        FlatMapChainBuilder chain = new FlatMapChainBuilder();
        chain.addOperation(
            "$T.deserialize($L)",
            TypeName.get(String.class),
            ClassName.get("com.example", "StringDeserializer"),
            "context"
        );
        
        CodeBlock result = chain.buildWithConstructor(
            me.bristermitten.mittenlib.util.Result.class,
            ClassName.get("com.example", "Config")
        );
        
        String code = result.toString();
        assertTrue(code.contains("StringDeserializer.deserialize(context)"));
    }
}
