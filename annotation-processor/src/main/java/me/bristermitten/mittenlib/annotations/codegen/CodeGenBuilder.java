package me.bristermitten.mittenlib.annotations.codegen;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;

/**
 * A higher-level builder for generating code blocks with better control flow tracking.
 * This class wraps JavaPoet's CodeBlock.Builder with additional functionality for
 * managing scope, control flow, and variable declarations.
 */
public class CodeGenBuilder {
    private final CodeBlock.Builder builder;
    private final Scope scope;
    private int indentLevel = 0;

    public CodeGenBuilder() {
        this(new Scope());
    }

    public CodeGenBuilder(Scope scope) {
        this.builder = CodeBlock.builder();
        this.scope = scope;
    }

    /**
     * Gets the current scope.
     *
     * @return The current scope
     */
    public Scope scope() {
        return scope;
    }

    /**
     * Gets the current indentation level.
     *
     * @return The indentation level
     */
    public int indentLevel() {
        return indentLevel;
    }

    /**
     * Adds a statement to the code block.
     *
     * @param format The format string
     * @param args   The arguments
     * @return This builder
     */
    public CodeGenBuilder addStatement(String format, Object... args) {
        builder.addStatement(format, args);
        return this;
    }

    /**
     * Adds a statement from a CodeBlock.
     *
     * @param statement The statement to add
     * @return This builder
     */
    public CodeGenBuilder addStatement(CodeBlock statement) {
        builder.addStatement(statement);
        return this;
    }

    /**
     * Adds code to the block.
     *
     * @param format The format string
     * @param args   The arguments
     * @return This builder
     */
    public CodeGenBuilder add(String format, Object... args) {
        builder.add(format, args);
        return this;
    }

    /**
     * Adds code from a CodeBlock.
     *
     * @param codeBlock The code to add
     * @return This builder
     */
    public CodeGenBuilder add(CodeBlock codeBlock) {
        builder.add(codeBlock);
        return this;
    }

    /**
     * Begins a control flow block (if, while, for, etc.).
     *
     * @param controlFlow The control flow statement
     * @param args        The arguments
     * @return This builder
     */
    public CodeGenBuilder beginControlFlow(String controlFlow, Object... args) {
        builder.beginControlFlow(controlFlow, args);
        indentLevel++;
        return this;
    }

    /**
     * Begins a control flow block using a CodeBlock.
     *
     * @param controlFlow The control flow statement
     * @return This builder
     */
    public CodeGenBuilder beginControlFlow(CodeBlock controlFlow) {
        builder.beginControlFlow(controlFlow);
        indentLevel++;
        return this;
    }

    /**
     * Ends the current control flow block.
     *
     * @return This builder
     */
    public CodeGenBuilder endControlFlow() {
        if (indentLevel <= 0) {
            throw new IllegalStateException("No control flow to end");
        }
        builder.endControlFlow();
        indentLevel--;
        return this;
    }

    /**
     * Adds an else-if control flow.
     *
     * @param controlFlow The control flow statement
     * @param args        The arguments
     * @return This builder
     */
    public CodeGenBuilder nextControlFlow(String controlFlow, Object... args) {
        builder.nextControlFlow(controlFlow, args);
        return this;
    }

    /**
     * Increases the indentation level.
     *
     * @return This builder
     */
    public CodeGenBuilder indent() {
        builder.indent();
        indentLevel++;
        return this;
    }

    /**
     * Decreases the indentation level.
     *
     * @return This builder
     */
    public CodeGenBuilder unindent() {
        if (indentLevel <= 0) {
            throw new IllegalStateException("Cannot unindent below zero");
        }
        builder.unindent();
        indentLevel--;
        return this;
    }

    /**
     * Declares a new variable in the current scope and adds a declaration statement.
     *
     * @param name         The variable name
     * @param type         The variable type
     * @param initializer  The initializer expression
     * @return The declared variable
     */
    public Variable declareVariable(String name, TypeName type, CodeBlock initializer) {
        Variable variable = scope.declare(name, type);
        addStatement("$T $N = $L", type, name, initializer);
        return variable;
    }

    /**
     * Declares a new variable in the current scope and adds a declaration statement.
     *
     * @param name         The variable name
     * @param type         The variable type
     * @param format       The initializer format string
     * @param args         The initializer arguments
     * @return The declared variable
     */
    public Variable declareVariable(String name, TypeName type, String format, Object... args) {
        return declareVariable(name, type, CodeBlock.of(format, args));
    }

    /**
     * Declares an anonymous variable with an auto-generated name.
     *
     * @param type        The variable type
     * @param initializer The initializer expression
     * @return The declared variable
     */
    public Variable declareAnonymous(TypeName type, CodeBlock initializer) {
        Variable variable = scope.declareAnonymous(type);
        addStatement("$T $N = $L", type, variable.name(), initializer);
        return variable;
    }

    /**
     * Builds the final CodeBlock.
     *
     * @return The built CodeBlock
     */
    public CodeBlock build() {
        if (indentLevel != 0) {
            throw new IllegalStateException("Unclosed control flow blocks: " + indentLevel);
        }
        return builder.build();
    }

    /**
     * Gets the underlying JavaPoet CodeBlock.Builder for direct access when needed.
     *
     * @return The underlying builder
     */
    public CodeBlock.Builder unwrap() {
        return builder;
    }
}
