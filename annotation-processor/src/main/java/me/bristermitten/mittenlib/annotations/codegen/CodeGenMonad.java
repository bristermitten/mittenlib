package me.bristermitten.mittenlib.annotations.codegen;

import com.squareup.javapoet.MethodSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * A functional, composable code generation monad that abstracts the pattern of
 * "try multiple strategies until one succeeds".
 * 
 * This is inspired by functional programming patterns like Option/Maybe monads
 * and provides a declarative way to build methods with multiple fallback strategies.
 * 
 * Can be used for any code generation scenario (deserialization, serialization, etc.)
 * 
 * Example usage:
 * <pre>
 * CodeGenMonad.builder(methodBuilder)
 *     .tryCase(() -> handleDirectMatch(...))
 *     .tryCase(() -> handleCustomDeserializer(...))
 *     .tryCase(() -> handleEnumType(...))
 *     .orElse(() -> handleFallback(...))
 *     .build();
 * </pre>
 */
public class CodeGenMonad {
    private final MethodSpec.Builder methodBuilder;
    private final List<Supplier<Boolean>> cases;
    private boolean built = false;
    
    private CodeGenMonad(MethodSpec.Builder methodBuilder) {
        this.methodBuilder = methodBuilder;
        this.cases = new ArrayList<>();
    }
    
    /**
     * Creates a new CodeGenMonad builder.
     * 
     * @param methodBuilder The method builder to work with
     * @return A new CodeGenMonad instance
     */
    public static CodeGenMonad builder(MethodSpec.Builder methodBuilder) {
        return new CodeGenMonad(methodBuilder);
    }
    
    /**
     * Adds a case to try. Cases are tried in the order they are added.
     * The case should return true if it handled the code generation, false otherwise.
     * 
     * @param caseHandler The case handler that returns true if successful
     * @return This monad for chaining
     */
    public CodeGenMonad tryCase(Supplier<Boolean> caseHandler) {
        if (built) {
            throw new IllegalStateException("Cannot add cases after building");
        }
        cases.add(caseHandler);
        return this;
    }
    
    /**
     * Adds a case that executes when a condition is true.
     * 
     * @param condition The condition to check
     * @param action The action to perform if condition is true
     * @return This monad for chaining
     */
    public CodeGenMonad tryCase(boolean condition, Runnable action) {
        return tryCase(() -> {
            if (condition) {
                action.run();
                return true;
            }
            return false;
        });
    }
    
    /**
     * Adds a terminal operation that is executed if no case succeeded.
     * This completes the monad and executes all pending operations.
     * 
     * @param terminal The terminal operation
     */
    public void orElse(Runnable terminal) {
        if (built) {
            throw new IllegalStateException("Already built");
        }
        built = true;
        
        // Try each case in order
        for (Supplier<Boolean> caseHandler : cases) {
            if (caseHandler.get()) {
                // Case succeeded, we're done
                return;
            }
        }
        
        // No case succeeded, execute terminal
        terminal.run();
    }
    
    /**
     * Completes the monad without a fallback.
     * Useful when all cases are expected to add their own return statements.
     */
    public void build() {
        if (built) {
            throw new IllegalStateException("Already built");
        }
        built = true;
        
        // Try each case in order
        for (Supplier<Boolean> caseHandler : cases) {
            if (caseHandler.get()) {
                // Case succeeded, we're done
                return;
            }
        }
    }
    
    /**
     * Gets the underlying MethodSpec.Builder for direct access.
     * 
     * @return The method builder
     */
    public MethodSpec.Builder getMethodBuilder() {
        return methodBuilder;
    }
    
    /**
     * Convenience method to create a case handler that executes when a condition is true.
     * 
     * @param condition The condition
     * @param action The action to perform if condition is true
     * @return A case handler
     */
    public static Supplier<Boolean> when(boolean condition, Runnable action) {
        return () -> {
            if (condition) {
                action.run();
                return true;
            }
            return false;
        };
    }
}
