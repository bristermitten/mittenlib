package me.bristermitten.mittenlib.annotations.codegen;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;

/**
 * Represents a variable in the generated code with its name and type.
 * This class provides a type-safe way to reference variables throughout code generation,
 * eliminating the need for hardcoded string variable names.
 */
public record Variable(String name, TypeName type) {
    /**
     * Creates a CodeBlock that references this variable.
     * Uses JavaPoet's $N format specifier which is designed for names
     * (variables, methods, types) and handles proper escaping.
     *
     * @return A CodeBlock containing this variable's name
     */
    public CodeBlock ref() {
        return CodeBlock.of("$N", name);
    }

    /**
     * Creates a CodeBlock that casts a value to this variable's type.
     *
     * @param value The value to cast
     * @return A CodeBlock with the cast expression
     */
    public CodeBlock cast(CodeBlock value) {
        return CodeBlock.of("($T) $L", type, value);
    }

    /**
     * Creates a CodeBlock that casts a value to this variable's type.
     *
     * @param value The value to cast
     * @return A CodeBlock with the cast expression
     */
    public CodeBlock cast(String value) {
        return cast(CodeBlock.of(value));
    }
}
