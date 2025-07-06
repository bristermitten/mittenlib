package me.bristermitten.mittenlib.annotations.config;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import me.bristermitten.mittenlib.annotations.ast.Property;

import javax.inject.Inject;
import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.Objects;

public class EqualsHashCodeGenerator {
    private final MethodNames methodNames;

    @Inject
    public EqualsHashCodeGenerator(MethodNames methodNames) {
        this.methodNames = methodNames;
    }

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
