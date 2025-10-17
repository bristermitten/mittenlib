package me.bristermitten.mittenlib.annotations.ast;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

/**
 * A representation of a property in a configuration type.
 * In practice, this is either an interface method or a field in a class.
 */
public record Property(
        String name,
        TypeMirror propertyType,
        PropertySource source,
        ASTSettings.PropertyASTSettings settings
) implements ASTNode {

    public sealed interface PropertySource {
        Element element();

        record FieldSource(VariableElement element) implements PropertySource {
        }

        record MethodSource(ExecutableElement element) implements PropertySource {
        }
    }
}
