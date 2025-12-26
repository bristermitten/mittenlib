package me.bristermitten.mittenlib.annotations.codegen;

import com.squareup.javapoet.MethodSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * A builder for creating deserialization method implementations using a strategy pattern.
 * Instead of returning booleans to indicate control flow, strategies are added declaratively
 * and the builder handles the control flow automatically.
 * 
 * Example usage:
 * <pre>
 * DeserializationMethodBuilder methodBuilder = new DeserializationMethodBuilder(builder);
 * methodBuilder
 *     .tryStrategy(() -> handleDirectTypeMatch(...))
 *     .tryStrategy(() -> handleDataTreeMatch(...))
 *     .tryStrategy(() -> handleCustomDeserializer(...))
 *     .orElse(() -> handleFallback(...));
 * </pre>
 */
public class DeserializationMethodBuilder {
    private final MethodSpec.Builder methodBuilder;
    private final List<DeserializationStrategy> strategies;
    private boolean built = false;
    
    /**
     * Represents a deserialization strategy that may or may not handle a case.
     */
    @FunctionalInterface
    public interface DeserializationStrategy {
        /**
         * Attempts to handle the deserialization.
         * 
         * @return true if this strategy handled the case and no further strategies should be tried,
         *         false if this strategy did not handle the case and the next strategy should be tried
         */
        boolean tryHandle();
    }
    
    /**
     * Represents a fallback deserialization handler that always completes the method.
     */
    @FunctionalInterface
    public interface FallbackHandler {
        /**
         * Handles the deserialization as a fallback.
         * This is always executed if no strategy succeeded.
         */
        void handle();
    }
    
    public DeserializationMethodBuilder(MethodSpec.Builder methodBuilder) {
        this.methodBuilder = methodBuilder;
        this.strategies = new ArrayList<>();
    }
    
    /**
     * Adds a strategy to try. Strategies are tried in the order they are added.
     * 
     * @param strategy The strategy to try
     * @return This builder for chaining
     */
    public DeserializationMethodBuilder tryStrategy(DeserializationStrategy strategy) {
        if (built) {
            throw new IllegalStateException("Cannot add strategies after building");
        }
        strategies.add(strategy);
        return this;
    }
    
    /**
     * Adds a fallback handler that is executed if no strategy succeeded.
     * This completes the method building process.
     * 
     * @param fallback The fallback handler
     */
    public void orElse(FallbackHandler fallback) {
        if (built) {
            throw new IllegalStateException("Method already built");
        }
        built = true;
        
        // Try each strategy in order
        for (DeserializationStrategy strategy : strategies) {
            if (strategy.tryHandle()) {
                // Strategy succeeded, we're done
                return;
            }
        }
        
        // No strategy succeeded, use fallback
        fallback.handle();
    }
    
    /**
     * Builds the method without a fallback.
     * This should only be used when all strategies are expected to add return statements.
     */
    public void build() {
        if (built) {
            throw new IllegalStateException("Method already built");
        }
        built = true;
        
        // Try each strategy in order
        for (DeserializationStrategy strategy : strategies) {
            if (strategy.tryHandle()) {
                // Strategy succeeded, we're done
                return;
            }
        }
    }
    
    /**
     * Gets the underlying MethodSpec.Builder for direct access when needed.
     * 
     * @return The method builder
     */
    public MethodSpec.Builder getMethodBuilder() {
        return methodBuilder;
    }
}
