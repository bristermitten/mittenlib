package me.bristermitten.mittenlib.annotations.codegen;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import org.junit.jupiter.api.Test;

import javax.lang.model.element.Modifier;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CodeGenMonadTest {

    @Test
    void testFirstCaseSucceeds() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("test")
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.VOID);
        
        AtomicInteger callCount = new AtomicInteger(0);
        
        CodeGenMonad.builder(builder)
            .tryCase(() -> {
                callCount.incrementAndGet();
                return true; // First case succeeds
            })
            .tryCase(() -> {
                callCount.incrementAndGet();
                return false; // Should not be called
            })
            .orElse(() -> {
                callCount.incrementAndGet(); // Should not be called
            });
        
        // Only first case should have been called
        assertEquals(1, callCount.get());
    }

    @Test
    void testTerminalExecuted() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("test")
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.VOID);
        
        AtomicBoolean terminalCalled = new AtomicBoolean(false);
        
        CodeGenMonad.builder(builder)
            .tryCase(() -> false) // Fails
            .tryCase(() -> false) // Fails
            .orElse(() -> {
                terminalCalled.set(true);
            });
        
        // Terminal should have been called
        assertTrue(terminalCalled.get());
    }

    @Test
    void testMiddleCaseSucceeds() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("test")
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.VOID);
        
        AtomicInteger callCount = new AtomicInteger(0);
        AtomicInteger successIndex = new AtomicInteger(-1);
        
        CodeGenMonad.builder(builder)
            .tryCase(() -> {
                callCount.incrementAndGet();
                return false; // First case fails
            })
            .tryCase(() -> {
                int index = callCount.incrementAndGet();
                successIndex.set(index);
                return true; // Second case succeeds
            })
            .tryCase(() -> {
                callCount.incrementAndGet();
                return false; // Should not be called
            })
            .orElse(() -> {
                callCount.incrementAndGet(); // Should not be called
            });
        
        // First two cases should have been called
        assertEquals(2, callCount.get());
        assertEquals(2, successIndex.get());
    }

    @Test
    void testBuildWithoutTerminal() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("test")
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.VOID);
        
        AtomicBoolean caseCalled = new AtomicBoolean(false);
        
        CodeGenMonad.builder(builder)
            .tryCase(() -> {
                caseCalled.set(true);
                return true;
            })
            .build();
        
        assertTrue(caseCalled.get());
    }

    @Test
    void testCannotAddAfterBuilding() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("test")
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.VOID);
        
        CodeGenMonad monad = CodeGenMonad.builder(builder);
        monad.tryCase(() -> true);
        monad.build();
        
        assertThrows(IllegalStateException.class, () -> {
            monad.tryCase(() -> false);
        });
    }

    @Test
    void testCannotBuildTwice() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("test")
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.VOID);
        
        CodeGenMonad monad = CodeGenMonad.builder(builder);
        monad.tryCase(() -> false);
        monad.orElse(() -> {});
        
        assertThrows(IllegalStateException.class, () -> {
            monad.build();
        });
    }

    @Test
    void testWhenHelper() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("test")
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.VOID);
        
        AtomicBoolean actionCalled = new AtomicBoolean(false);
        
        CodeGenMonad.builder(builder)
            .tryCase(CodeGenMonad.when(true, () -> actionCalled.set(true)))
            .build();
        
        assertTrue(actionCalled.get());
    }

    @Test
    void testWhenHelperFalse() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("test")
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.VOID);
        
        AtomicBoolean actionCalled = new AtomicBoolean(false);
        AtomicBoolean terminalCalled = new AtomicBoolean(false);
        
        CodeGenMonad.builder(builder)
            .tryCase(CodeGenMonad.when(false, () -> actionCalled.set(true)))
            .orElse(() -> terminalCalled.set(true));
        
        assertFalse(actionCalled.get());
        assertTrue(terminalCalled.get());
    }

    @Test
    void testTryCaseWithCondition() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("test")
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.VOID);
        
        AtomicBoolean actionCalled = new AtomicBoolean(false);
        
        CodeGenMonad.builder(builder)
            .tryCase(true, () -> actionCalled.set(true))
            .build();
        
        assertTrue(actionCalled.get());
    }

    @Test
    void testTryCaseWithConditionFalse() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("test")
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.VOID);
        
        AtomicBoolean actionCalled = new AtomicBoolean(false);
        AtomicBoolean terminalCalled = new AtomicBoolean(false);
        
        CodeGenMonad.builder(builder)
            .tryCase(false, () -> actionCalled.set(true))
            .orElse(() -> terminalCalled.set(true));
        
        assertFalse(actionCalled.get());
        assertTrue(terminalCalled.get());
    }

    @Test
    void testGetMethodBuilder() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("test")
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.VOID);
        
        CodeGenMonad monad = CodeGenMonad.builder(builder);
        
        assertSame(builder, monad.getMethodBuilder());
    }
}
