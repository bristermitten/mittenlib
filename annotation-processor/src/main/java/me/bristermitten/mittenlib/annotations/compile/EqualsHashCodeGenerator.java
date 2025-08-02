package me.bristermitten.mittenlib.annotations.compile;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import me.bristermitten.mittenlib.annotations.ast.Property;

import javax.inject.Inject;
import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.Objects;

/**
 * Generates equals and hashCode methods for configuration classes.
 * This class creates standard implementations that compare all properties
 * of a configuration class for equality and generate consistent hash codes.
 */
public class EqualsHashCodeGenerator {
    private final MethodNames methodNames;

    @Inject
    public EqualsHashCodeGenerator(MethodNames methodNames) {
        this.methodNames = methodNames;
    }

    /**
     * Generates an equals method for a configuration class.
     * The generated method follows the standard equals contract:
     * - It's reflexive: an object is equal to itself
     * - It handles null and different class types
     * - It compares all properties for equality using Objects.equals
     *
     * @param configClassName The name of the class for which the equals method is being generated
     * @param properties      The list of properties to compare in the equals method
     * @return A MethodSpec representing the generated equals method
     */
    public MethodSpec generateEquals(ClassName configClassName, List<Property> properties) {
        var builder = MethodSpec.methodBuilder("equals")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(boolean.class)
                .addParameter(Object.class, "o");

        builder.beginControlFlow("if (this == o)")
                .addStatement("return true")
                .endControlFlow();

        builder.beginControlFlow("if (o == null || getClass() != o.getClass())")
                .addStatement("return false")
                .endControlFlow();

        builder.addStatement("$T that = ($T) o", configClassName, configClassName);

        for (Property property : properties) {
            builder.addStatement("if (!$T.equals(this.$L(), that.$L())) return false",
                    Objects.class,
                    methodNames.safeMethodName(property),
                    methodNames.safeMethodName(property));
        }

        builder.addStatement("return true");

        return builder.build();
    }

    /**
     * Generates a hashCode method for a configuration class.
     * The generated method uses Objects.hash to create a hash code based on all properties,
     * ensuring that objects that are equal according to equals() will have the same hash code.
     *
     * @param properties The list of properties to include in the hash code calculation
     * @return A MethodSpec representing the generated hashCode method
     */
    public MethodSpec generateHashCode(List<Property> properties) {
        var builder = MethodSpec.methodBuilder("hashCode")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(int.class);

        builder.addStatement("return $T.hash($L)", Objects.class,
                properties.stream()
                        .map(property -> "this." + methodNames.safeMethodName(property) + "()")
                        .reduce((a, b) -> a + ", " + b)
                        .orElse(""));

        return builder.build();
    }
}
