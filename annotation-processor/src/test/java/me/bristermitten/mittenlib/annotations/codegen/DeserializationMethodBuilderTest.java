package me.bristermitten.mittenlib.annotations.codegen;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import org.junit.jupiter.api.Test;

import javax.lang.model.element.Modifier;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class DeserializationMethodBuilderTest {

    @Test
    void testFirstStrategySucceeds() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("test")
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.VOID);
        
        AtomicInteger callCount = new AtomicInteger(0);
        
        DeserializationMethodBuilder methodBuilder = new DeserializationMethodBuilder(builder);
        methodBuilder
            .tryStrategy(() -> {
                callCount.incrementAndGet();
                return true; // First strategy succeeds
            })
            .tryStrategy(() -> {
                callCount.incrementAndGet();
                return false; // Should not be called
            })
            .orElse(() -> {
                callCount.incrementAndGet(); // Should not be called
            });
        
        // Only first strategy should have been called
        assertEquals(1, callCount.get());
    }

    @Test
    void testFallbackExecuted() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("test")
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.VOID);
        
        AtomicBoolean fallbackCalled = new AtomicBoolean(false);
        
        DeserializationMethodBuilder methodBuilder = new DeserializationMethodBuilder(builder);
        methodBuilder
            .tryStrategy(() -> false) // Fails
            .tryStrategy(() -> false) // Fails
            .orElse(() -> {
                fallbackCalled.set(true);
            });
        
        // Fallback should have been called
        assertTrue(fallbackCalled.get());
    }

    @Test
    void testMiddleStrategySucceeds() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("test")
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.VOID);
        
        AtomicInteger callCount = new AtomicInteger(0);
        AtomicInteger successIndex = new AtomicInteger(-1);
        
        DeserializationMethodBuilder methodBuilder = new DeserializationMethodBuilder(builder);
        methodBuilder
            .tryStrategy(() -> {
                callCount.incrementAndGet();
                return false; // First strategy fails
            })
            .tryStrategy(() -> {
                int index = callCount.incrementAndGet();
                successIndex.set(index);
                return true; // Second strategy succeeds
            })
            .tryStrategy(() -> {
                callCount.incrementAndGet();
                return false; // Should not be called
            })
            .orElse(() -> {
                callCount.incrementAndGet(); // Should not be called
            });
        
        // First two strategies should have been called
        assertEquals(2, callCount.get());
        assertEquals(2, successIndex.get());
    }

    @Test
    void testBuildWithoutFallback() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("test")
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.VOID);
        
        AtomicBoolean strategyCalled = new AtomicBoolean(false);
        
        DeserializationMethodBuilder methodBuilder = new DeserializationMethodBuilder(builder);
        methodBuilder
            .tryStrategy(() -> {
                strategyCalled.set(true);
                return true;
            });
        
        methodBuilder.build();
        
        assertTrue(strategyCalled.get());
    }

    @Test
    void testCannotAddAfterBuilding() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("test")
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.VOID);
        
        DeserializationMethodBuilder methodBuilder = new DeserializationMethodBuilder(builder);
        methodBuilder.tryStrategy(() -> true);
        methodBuilder.build();
        
        assertThrows(IllegalStateException.class, () -> {
            methodBuilder.tryStrategy(() -> false);
        });
    }

    @Test
    void testCannotBuildTwice() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("test")
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.VOID);
        
        DeserializationMethodBuilder methodBuilder = new DeserializationMethodBuilder(builder);
        methodBuilder.tryStrategy(() -> false);
        methodBuilder.orElse(() -> {});
        
        assertThrows(IllegalStateException.class, () -> {
            methodBuilder.build();
        });
    }

    @Test
    void testGetMethodBuilder() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("test")
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.VOID);
        
        DeserializationMethodBuilder methodBuilder = new DeserializationMethodBuilder(builder);
        
        assertSame(builder, methodBuilder.getMethodBuilder());
    }
}
