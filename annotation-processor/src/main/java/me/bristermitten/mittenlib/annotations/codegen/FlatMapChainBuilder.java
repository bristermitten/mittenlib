package me.bristermitten.mittenlib.annotations.codegen;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;

import java.util.ArrayList;
import java.util.List;

/**
 * A high-level builder for creating chained flatMap expressions.
 * Automatically manages variable scoping, naming, and parentheses balancing.
 * 
 * Example usage:
 * <pre>
 * FlatMapChainBuilder chain = new FlatMapChainBuilder();
 * chain.addOperation("deserializeX(context)", TypeName.get(String.class));
 * chain.addOperation("deserializeY(context)", TypeName.INT);
 * CodeBlock result = chain.buildWithConstructor(Result.class, ConfigImpl.class);
 * // Generates: return deserializeX(context).flatMap(var0 -> 
 * //            deserializeY(context).flatMap(var1 ->
 * //            Result.ok(new ConfigImpl(var0, var1))))
 * </pre>
 */
public class FlatMapChainBuilder {
    private final Scope scope;
    private final List<ChainOperation> operations;
    
    /**
     * Represents a single operation in the flatMap chain.
     */
    private record ChainOperation(CodeBlock expression, Variable resultVariable) {}
    
    public FlatMapChainBuilder() {
        this(new Scope());
    }
    
    public FlatMapChainBuilder(Scope scope) {
        this.scope = scope;
        this.operations = new ArrayList<>();
    }
    
    /**
     * Adds an operation to the chain.
     * The operation should return Result&lt;T&gt; where T is the resultType.
     * A variable will be automatically created to hold the result.
     *
     * @param expression The expression to execute (e.g., "deserializeX(context)")
     * @param resultType The type of the successful result (T in Result&lt;T&gt;)
     * @return This builder for chaining
     */
    public FlatMapChainBuilder addOperation(CodeBlock expression, TypeName resultType) {
        Variable resultVar = scope.declareAnonymous(resultType);
        operations.add(new ChainOperation(expression, resultVar));
        return this;
    }
    
    /**
     * Adds an operation to the chain using a string format.
     *
     * @param format The format string
     * @param resultType The type of the successful result
     * @param args Format arguments
     * @return This builder for chaining
     */
    public FlatMapChainBuilder addOperation(String format, TypeName resultType, Object... args) {
        return addOperation(CodeBlock.of(format, args), resultType);
    }
    
    /**
     * Builds the complete flatMap chain that ends with a constructor call.
     * Automatically manages variable scoping and parentheses balancing.
     *
     * @param resultClass The Result class
     * @param constructorClass The class to construct with all accumulated variables
     * @return The complete CodeBlock
     */
    public CodeBlock buildWithConstructor(Class<?> resultClass, TypeName constructorClass) {
        if (operations.isEmpty()) {
            throw new IllegalStateException("No operations added to chain");
        }
        
        CodeBlock.Builder builder = CodeBlock.builder();
        builder.add("return ");
        
        // Add each operation with flatMap
        for (ChainOperation op : operations) {
            builder.add("$L.flatMap($L -> \n", op.expression, op.resultVariable.name());
        }
        
        // Add the final constructor call
        builder.add("$T.ok(new $T(", resultClass, constructorClass);
        
        // Add all variables as constructor parameters
        List<Variable> allVars = scope.allVariables();
        for (int i = 0; i < allVars.size(); i++) {
            builder.add("$L", allVars.get(i).name());
            if (i != allVars.size() - 1) {
                builder.add(", ");
            }
        }
        
        builder.add("))"); // Close ok() and new
        builder.add(")".repeat(operations.size())); // Close all flatMaps
        
        return builder.build();
    }
    
    /**
     * Gets the scope used by this chain builder.
     *
     * @return The scope
     */
    public Scope getScope() {
        return scope;
    }
    
    /**
     * Gets all variables accumulated in this chain.
     *
     * @return List of all variables in order
     */
    public List<Variable> getVariables() {
        return operations.stream()
                .map(ChainOperation::resultVariable)
                .toList();
    }
}
