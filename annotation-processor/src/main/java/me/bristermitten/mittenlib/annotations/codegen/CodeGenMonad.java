package me.bristermitten.mittenlib.annotations.codegen;

import com.squareup.javapoet.MethodSpec;
import me.bristermitten.mittenlib.annotations.codegen.CodeGenDSL.CodeGenResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A functional, composable code generation monad that abstracts the pattern of
 * "try multiple strategies until one succeeds".
 * 
 * This monad now works with pure CodeGenResult operations, separating the
 * specification of code generation from its execution.
 * 
 * Can be used for any code generation scenario (deserialization, serialization, etc.)
 * 
 * Example usage (pure):
 * <pre>
 * CodeGenMonad.pure(methodBuilder)
 *     .tryCase(() -> CodeGenDSL.statement("return $T.success($L)", Result.class, "value"))
 *     .tryCase(() -> CodeGenDSL.controlFlow("if ($L)", "condition")
 *                       .addReturn("$T.error($S)", Result.class, "Failed")
 *                       .build())
 *     .orElse(() -> CodeGenDSL.returnValue("$T.empty()", Result.class))
 *     .apply();
 * </pre>
 */
public class CodeGenMonad {
    private final MethodSpec.Builder methodBuilder;
    private final List<Supplier<Optional<CodeGenResult>>> cases;
    private boolean built = false;
    
    private CodeGenMonad(MethodSpec.Builder methodBuilder) {
        this.methodBuilder = methodBuilder;
        this.cases = new ArrayList<>();
    }
    
    /**
     * Creates a new pure CodeGenMonad that works with immutable CodeGenResult operations.
     * 
     * @param methodBuilder The method builder (only mutated at apply() boundary)
     * @return A new CodeGenMonad instance
     */
    public static CodeGenMonad pure(MethodSpec.Builder methodBuilder) {
        return new CodeGenMonad(methodBuilder);
    }
    
    /**
     * Legacy builder method for backward compatibility.
     * Prefer using {@link #pure(MethodSpec.Builder)} for new code.
     * 
     * @param methodBuilder The method builder to work with
     * @return A new CodeGenMonad instance
     */
    public static CodeGenMonad builder(MethodSpec.Builder methodBuilder) {
        return new CodeGenMonad(methodBuilder);
    }
    
    /**
     * Adds a pure case to try. Cases are tried in the order they are added.
     * The case should return a CodeGenResult if it handles the generation,
     * or Optional.empty() otherwise.
     * 
     * @param caseHandler The case handler that returns Optional<CodeGenResult>
     * @return This monad for chaining
     */
    public CodeGenMonad tryCase(Supplier<Optional<CodeGenResult>> caseHandler) {
        if (built) {
            throw new IllegalStateException("Cannot add cases after building");
        }
        cases.add(caseHandler);
        return this;
    }
    
    /**
     * Adds a pure case that returns a result directly.
     * 
     * @param caseHandler The case handler that returns CodeGenResult
     * @return This monad for chaining
     */
    public CodeGenMonad tryCaseDirect(Supplier<CodeGenResult> caseHandler) {
        return tryCase(() -> Optional.of(caseHandler.get()));
    }
    
    /**
     * Adds a case that executes when a condition is true (pure version).
     * 
     * @param condition The condition to check
     * @param resultSupplier The supplier of CodeGenResult if condition is true
     * @return This monad for chaining
     */
    public CodeGenMonad tryCase(boolean condition, Supplier<CodeGenResult> resultSupplier) {
        return tryCase(() -> condition ? Optional.of(resultSupplier.get()) : Optional.empty());
    }
    
    /**
     * Adds a terminal operation that is executed if no case succeeded.
     * This completes the monad and applies all operations to the builder.
     * 
     * @param terminal The terminal operation supplier
     */
    public void orElse(Supplier<CodeGenResult> terminal) {
        if (built) {
            throw new IllegalStateException("Already built");
        }
        built = true;
        
        // Try each case in order
        for (Supplier<Optional<CodeGenResult>> caseHandler : cases) {
            Optional<CodeGenResult> result = caseHandler.get();
            if (result.isPresent()) {
                // Case succeeded, apply and we're done
                result.get().apply(methodBuilder);
                return;
            }
        }
        
        // No case succeeded, execute terminal
        terminal.get().apply(methodBuilder);
    }
    
    /**
     * Applies the monad without a fallback.
     * The first successful case will be applied.
     * 
     * @throws IllegalStateException if no case succeeds (ensure at least one case matches or use orElse)
     */
    public void apply() {
        if (built) {
            throw new IllegalStateException("Already built");
        }
        built = true;
        
        // Try each case in order
        for (Supplier<Optional<CodeGenResult>> caseHandler : cases) {
            Optional<CodeGenResult> result = caseHandler.get();
            if (result.isPresent()) {
                // Case succeeded, apply and we're done
                result.get().apply(methodBuilder);
                return;
            }
        }
        
        // No case succeeded; this likely indicates a code generation error
        throw new IllegalStateException("No code generation case succeeded; " +
                "either add a matching case or use orElse(...) to provide a fallback.");
    }
    
    /**
     * Legacy method for backward compatibility with impure code.
     * 
     * @param caseHandler The case handler that returns true if successful
     * @return This monad for chaining
     * @deprecated Use pure version with CodeGenResult instead
     */
    @Deprecated
    public CodeGenMonad tryCaseImpure(Supplier<Boolean> caseHandler) {
        return tryCase(() -> {
            if (caseHandler.get()) {
                return Optional.of(CodeGenDSL.empty());
            }
            return Optional.empty();
        });
    }
    
    /**
     * Legacy method for backward compatibility with impure code.
     * 
     * @param condition The condition to check
     * @param action The action to perform if condition is true
     * @return This monad for chaining
     * @deprecated Use pure version with CodeGenResult instead
     */
    @Deprecated
    public CodeGenMonad tryCaseImpure(boolean condition, Runnable action) {
        return tryCaseImpure(() -> {
            if (condition) {
                action.run();
                return true;
            }
            return false;
        });
    }
    
    /**
     * Legacy method for backward compatibility with impure code.
     * 
     * @param terminal The terminal operation
     * @deprecated Use pure version with CodeGenResult instead
     */
    @Deprecated
    public void orElseImpure(Runnable terminal) {
        orElse(() -> {
            terminal.run();
            return CodeGenDSL.empty();
        });
    }
}
