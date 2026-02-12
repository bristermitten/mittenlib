package me.bristermitten.mittenlib.annotations.codegen;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import me.bristermitten.mittenlib.annotations.codegen.CodeGenDSL.CodeGenResult;
import org.junit.jupiter.api.Test;

import javax.lang.model.element.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class CodeGenDSLTest {

    @Test
    void testStatementCreation() {
        CodeGenResult result = CodeGenDSL.statement("int x = $L", 42);
        assertNotNull(result);
    }

    @Test
    void testStatementApplication() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("test")
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class);

        CodeGenResult result = CodeGenDSL.statement("int x = $L", 42);
        result.apply(builder);

        MethodSpec method = builder.build();
        String code = method.toString();
        assertTrue(code.contains("int x = 42"));
    }

    @Test
    void testReturnValue() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("test")
                .addModifiers(Modifier.PUBLIC)
                .returns(int.class);

        CodeGenResult result = CodeGenDSL.returnValue("$L", 42);
        result.apply(builder);

        MethodSpec method = builder.build();
        String code = method.toString();
        assertTrue(code.contains("return 42"));
    }

    @Test
    void testEmpty() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("test")
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class);

        CodeGenResult result = CodeGenDSL.empty();
        result.apply(builder);

        MethodSpec method = builder.build();
        String code = method.toString();
        // Should not add any statements
        assertFalse(code.contains("return"));
    }

    @Test
    void testControlFlow() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("test")
                .addModifiers(Modifier.PUBLIC)
                .returns(int.class)
                .addParameter(boolean.class, "condition");

        CodeGenResult result = CodeGenDSL.controlFlow("if ($L)", "condition")
                .addReturn("$L", 1)
                .build();
        result.apply(builder);

        MethodSpec method = builder.build();
        String code = method.toString();
        assertTrue(code.contains("if (condition)"));
        assertTrue(code.contains("return 1"));
    }

    @Test
    void testControlFlowWithMultipleStatements() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("test")
                .addModifiers(Modifier.PUBLIC)
                .returns(int.class)
                .addParameter(boolean.class, "condition");

        CodeGenResult result = CodeGenDSL.controlFlow("if ($L)", "condition")
                .addStatement("int x = $L", 10)
                .addStatement("int y = $L", 20)
                .addReturn("x + y")
                .build();
        result.apply(builder);

        MethodSpec method = builder.build();
        String code = method.toString();
        assertTrue(code.contains("if (condition)"));
        assertTrue(code.contains("int x = 10"));
        assertTrue(code.contains("int y = 20"));
        assertTrue(code.contains("return x + y"));
    }

    @Test
    void testCombine() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("test")
                .addModifiers(Modifier.PUBLIC)
                .returns(int.class);

        CodeGenResult result1 = CodeGenDSL.statement("int x = $L", 10);
        CodeGenResult result2 = CodeGenDSL.statement("int y = $L", 20);
        CodeGenResult combined = result1.combine(result2);

        combined.apply(builder);

        MethodSpec method = builder.build();
        String code = method.toString();
        assertTrue(code.contains("int x = 10"));
        assertTrue(code.contains("int y = 20"));
    }

    @Test
    void testMultipleCombine() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("test")
                .addModifiers(Modifier.PUBLIC)
                .returns(int.class);

        CodeGenResult result = CodeGenDSL.statement("int x = $L", 10)
                .combine(CodeGenDSL.statement("int y = $L", 20))
                .combine(CodeGenDSL.returnValue("x + y"));

        result.apply(builder);

        MethodSpec method = builder.build();
        String code = method.toString();
        assertTrue(code.contains("int x = 10"));
        assertTrue(code.contains("int y = 20"));
        assertTrue(code.contains("return x + y"));
    }

    @Test
    void testControlFlowWithNestedResult() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("test")
                .addModifiers(Modifier.PUBLIC)
                .returns(int.class)
                .addParameter(boolean.class, "condition");

        CodeGenResult nested = CodeGenDSL.statement("int x = $L", 10)
                .combine(CodeGenDSL.returnValue("x"));

        CodeGenResult result = CodeGenDSL.controlFlow("if ($L)", "condition")
                .add(nested)
                .build();

        result.apply(builder);

        MethodSpec method = builder.build();
        String code = method.toString();
        assertTrue(code.contains("if (condition)"));
        assertTrue(code.contains("int x = 10"));
        assertTrue(code.contains("return x"));
    }

    @Test
    void testPureComposition() {
        // Test that results can be created and composed without any side effects
        CodeGenResult result1 = CodeGenDSL.statement("int x = $L", 10);
        CodeGenResult result2 = CodeGenDSL.statement("int y = $L", 20);
        CodeGenResult combined = result1.combine(result2);

        // At this point, nothing has been applied - it's just data
        assertNotNull(combined);

        // Only when we apply does mutation happen
        MethodSpec.Builder builder = MethodSpec.methodBuilder("test")
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class);

        combined.apply(builder);

        MethodSpec method = builder.build();
        String code = method.toString();
        assertTrue(code.contains("int x = 10"));
        assertTrue(code.contains("int y = 20"));
    }
}
