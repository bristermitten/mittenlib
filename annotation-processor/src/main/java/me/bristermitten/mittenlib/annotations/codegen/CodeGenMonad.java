package me.bristermitten.mittenlib.annotations.codegen;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
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
    private final List<Case> cases;
    private boolean built = false;
    
    /**
     * Represents a case that may or may not handle the code generation.
     */
    @FunctionalInterface
    public interface Case {
        /**
         * Attempts to handle the code generation case.
         * 
         * @return Optional.of(result) if this case handled it, Optional.empty() otherwise
         */
        Optional<Void> tryHandle();
    }
    
    /**
     * A terminal operation that always completes the code generation.
     */
    @FunctionalInterface
    public interface Terminal {
        /**
         * Completes the code generation.
         */
        void complete();
    }
    
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
     * 
     * @param caseHandler The case handler
     * @return This monad for chaining
     */
    public CodeGenMonad tryCase(Case caseHandler) {
        if (built) {
            throw new IllegalStateException("Cannot add cases after building");
        }
        cases.add(caseHandler);
        return this;
    }
    
    /**
     * Adds a case using a boolean-returning function.
     * This is a convenience method for legacy code.
     * 
     * @param condition The condition to check
     * @return This monad for chaining
     */
    public CodeGenMonad tryCase(Supplier<Boolean> condition) {
        return tryCase(() -> condition.get() ? Optional.of(null) : Optional.empty());
    }
    
    /**
     * Adds a terminal operation that is executed if no case succeeded.
     * This completes the monad and executes all pending operations.
     * 
     * @param terminal The terminal operation
     */
    public void orElse(Terminal terminal) {
        if (built) {
            throw new IllegalStateException("Already built");
        }
        built = true;
        
        // Try each case in order
        for (Case caseHandler : cases) {
            Optional<Void> result = caseHandler.tryHandle();
            if (result.isPresent()) {
                // Case succeeded, we're done
                return;
            }
        }
        
        // No case succeeded, execute terminal
        terminal.complete();
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
        for (Case caseHandler : cases) {
            Optional<Void> result = caseHandler.tryHandle();
            if (result.isPresent()) {
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
     * Convenience method to create a Case that returns success if a boolean condition is true.
     * 
     * @param condition The condition
     * @param action The action to perform if condition is true
     * @return A Case
     */
    public static Case when(boolean condition, Runnable action) {
        return () -> {
            if (condition) {
                action.run();
                return Optional.of(null);
            }
            return Optional.empty();
        };
    }
    
    /**
     * Convenience method to create a Case from a boolean supplier.
     * 
     * @param supplier The supplier that returns true if it handled the case
     * @return A Case
     */
    public static Case fromBoolean(Supplier<Boolean> supplier) {
        return () -> supplier.get() ? Optional.of(null) : Optional.empty();
    }
}
