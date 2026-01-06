package me.bristermitten.mittenlib.annotations.codegen;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A pure, declarative DSL for code generation that separates the specification
 * of code operations from their execution.
 * 
 * This DSL represents code generation operations as immutable data structures,
 * allowing for functional composition and reasoning. The impure boundary is
 * isolated to the {@link CodeGenResult#apply(MethodSpec.Builder)} method.
 * 
 * Example usage:
 * <pre>
 * CodeGenResult result = CodeGenDSL.statement("return $T.success($L)", Result.class, "value");
 * result.apply(builder);  // Single point of mutation
 * </pre>
 */
public class CodeGenDSL {
    
    private CodeGenDSL() {
        // Utility class
    }
    
    /**
     * Creates a statement operation.
     * 
     * @param format The format string
     * @param args The format arguments
     * @return An immutable CodeGenResult representing this statement
     */
    public static CodeGenResult statement(String format, Object... args) {
        return new Statement(CodeBlock.of(format, args));
    }
    
    /**
     * Creates a return statement operation.
     * 
     * @param format The format string for the return expression
     * @param args The format arguments
     * @return An immutable CodeGenResult representing this return
     */
    public static CodeGenResult returnValue(String format, Object... args) {
        return new Return(CodeBlock.of(format, args));
    }
    
    /**
     * Creates an empty operation (identity element).
     * 
     * @return An empty CodeGenResult
     */
    public static CodeGenResult empty() {
        return new Empty();
    }
    
    /**
     * Creates an operation from a pre-built CodeBlock.
     * Useful for complex code that's already been constructed.
     * 
     * @param code The code block to add as a statement
     * @return An immutable CodeGenResult representing this code
     */
    public static CodeGenResult addCode(CodeBlock code) {
        return new Statement(code);
    }
    
    /**
     * Begins a control flow block (if, for, while, etc.) using a builder.
     * 
     * @param format The control flow format string
     * @param args The format arguments
     * @return A builder for constructing the control flow block
     * @deprecated Use {@link #controlFlow(String, java.util.function.Consumer, Object...)} for lambda-based API
     */
    @Deprecated
    public static ControlFlowBuilder controlFlow(String format, Object... args) {
        return new ControlFlowBuilder(CodeBlock.of(format, args));
    }
    
    /**
     * Creates a control flow block (if, for, while, etc.) using a lambda to define the body.
     * This mirrors the actual control flow structure more naturally.
     * 
     * Example:
     * <pre>
     * controlFlow("if ($L != null)", body -> {
     *     body.add(statement("return $T.success($L)", Result.class, "value"));
     * }, varName)
     * </pre>
     * 
     * @param format The control flow format string
     * @param bodyBuilder Consumer that builds the body using a ControlFlowBodyBuilder
     * @param args The format arguments
     * @return An immutable CodeGenResult representing this control flow
     */
    public static CodeGenResult controlFlow(String format, java.util.function.Consumer<ControlFlowBodyBuilder> bodyBuilder, Object... args) {
        ControlFlowBodyBuilder builder = new ControlFlowBodyBuilder();
        bodyBuilder.accept(builder);
        return new ControlFlow(CodeBlock.of(format, args), builder.getBody());
    }
    
    /**
     * Represents the result of a code generation operation.
     * This is an immutable data structure that can be composed functionally.
     */
    public interface CodeGenResult {
        /**
         * Applies this result to a method builder (the impure boundary).
         * 
         * @param builder The method builder to apply to
         */
        void apply(MethodSpec.Builder builder);
        
        /**
         * Combines this result with another result.
         * 
         * @param other The other result to combine with
         * @return A new combined result
         */
        default CodeGenResult combine(CodeGenResult other) {
            return new Combined(List.of(this, other));
        }
    }
    
    /**
     * Represents a statement operation.
     */
    private static class Statement implements CodeGenResult {
        private final CodeBlock code;
        
        Statement(CodeBlock code) {
            this.code = code;
        }
        
        @Override
        public void apply(MethodSpec.Builder builder) {
            builder.addStatement(code);
        }
    }
    
    /**
     * Represents a return statement operation.
     */
    private static class Return implements CodeGenResult {
        private final CodeBlock expression;
        
        Return(CodeBlock expression) {
            this.expression = expression;
        }
        
        @Override
        public void apply(MethodSpec.Builder builder) {
            builder.addStatement("return $L", expression);
        }
    }
    
    /**
     * Represents an empty operation (identity element for composition).
     */
    private static class Empty implements CodeGenResult {
        @Override
        public void apply(MethodSpec.Builder builder) {
            // No operation
        }
    }
    
    /**
     * Represents a control flow block.
     */
    private static class ControlFlow implements CodeGenResult {
        private final CodeBlock controlStatement;
        private final List<CodeGenResult> body;
        
        ControlFlow(CodeBlock controlStatement, List<CodeGenResult> body) {
            this.controlStatement = controlStatement;
            this.body = Collections.unmodifiableList(new ArrayList<>(body));
        }
        
        @Override
        public void apply(MethodSpec.Builder builder) {
            builder.beginControlFlow(controlStatement.toString());
            for (CodeGenResult operation : body) {
                operation.apply(builder);
            }
            builder.endControlFlow();
        }
    }
    
    /**
     * Represents multiple operations combined together.
     */
    private static class Combined implements CodeGenResult {
        private final List<CodeGenResult> operations;
        
        Combined(List<CodeGenResult> operations) {
            this.operations = Collections.unmodifiableList(new ArrayList<>(operations));
        }
        
        @Override
        public void apply(MethodSpec.Builder builder) {
            for (CodeGenResult operation : operations) {
                operation.apply(builder);
            }
        }
        
        @Override
        public CodeGenResult combine(CodeGenResult other) {
            List<CodeGenResult> combined = new ArrayList<>(operations);
            combined.add(other);
            return new Combined(combined);
        }
    }
    
    /**
     * Builder for control flow blocks (legacy builder pattern).
     * @deprecated Use lambda-based controlFlow method instead
     */
    @Deprecated
    public static class ControlFlowBuilder {
        private final CodeBlock controlStatement;
        private final List<CodeGenResult> body = new ArrayList<>();
        
        ControlFlowBuilder(CodeBlock controlStatement) {
            this.controlStatement = controlStatement;
        }
        
        /**
         * Adds a statement to the control flow body.
         * 
         * @param format The format string
         * @param args The format arguments
         * @return This builder for chaining
         */
        public ControlFlowBuilder addStatement(String format, Object... args) {
            body.add(new Statement(CodeBlock.of(format, args)));
            return this;
        }
        
        /**
         * Adds a return statement to the control flow body.
         * 
         * @param format The format string
         * @param args The format arguments
         * @return This builder for chaining
         */
        public ControlFlowBuilder addReturn(String format, Object... args) {
            body.add(new Return(CodeBlock.of(format, args)));
            return this;
        }
        
        /**
         * Adds an arbitrary result to the control flow body.
         * 
         * @param result The result to add
         * @return This builder for chaining
         */
        public ControlFlowBuilder add(CodeGenResult result) {
            body.add(result);
            return this;
        }
        
        /**
         * Builds the control flow as an immutable CodeGenResult.
         * 
         * @return The immutable control flow result
         */
        public CodeGenResult build() {
            return new ControlFlow(controlStatement, body);
        }
    }
    
    /**
     * Builder for control flow body using lambda-based API.
     * This is used internally by the lambda-based controlFlow method.
     */
    public static class ControlFlowBodyBuilder {
        private final List<CodeGenResult> body = new ArrayList<>();
        
        /**
         * Adds a result to the control flow body.
         * 
         * @param result The result to add
         */
        public void add(CodeGenResult result) {
            body.add(result);
        }
        
        /**
         * Adds a statement to the control flow body.
         * 
         * @param format The format string
         * @param args The format arguments
         */
        public void addStatement(String format, Object... args) {
            body.add(new Statement(CodeBlock.of(format, args)));
        }
        
        /**
         * Adds a return statement to the control flow body.
         * 
         * @param format The format string
         * @param args The format arguments
         */
        public void addReturn(String format, Object... args) {
            body.add(new Return(CodeBlock.of(format, args)));
        }
        
        List<CodeGenResult> getBody() {
            return body;
        }
    }
}
