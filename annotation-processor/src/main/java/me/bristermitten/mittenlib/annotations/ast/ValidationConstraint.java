package me.bristermitten.mittenlib.annotations.ast;

import com.squareup.javapoet.ClassName;

/**
 * Information about a constraint placed upon a config property.
 */
public sealed interface ValidationConstraint {
    /**
     * The numeric value must be positive ({@code > 0})
     */
    record Positive() implements ValidationConstraint {}

    /**
     * The numeric value must be negative ({@code < 0})
     */
    record Negative() implements ValidationConstraint {}
    record Min(double value) implements ValidationConstraint {}
    record Max(double value) implements ValidationConstraint {}
    record NotBlank() implements ValidationConstraint {}
    record Range(double min, double max) implements ValidationConstraint {}
    record Custom(ClassName validatorClassName) implements ValidationConstraint {}
}
