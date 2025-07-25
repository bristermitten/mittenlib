package me.bristermitten.mittenlib.codegen;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import me.bristermitten.mittenlib.codegen.record.RecordConstructorSpec;

import javax.lang.model.element.Modifier;
import java.util.Objects;

public class BoilerplateGenerator {
    public static MethodSpec genEquals(RecordConstructorSpec recordConstructorSpec, ClassName name) {
        MethodSpec.Builder equalsBuilder = MethodSpec.methodBuilder("equals")
                .addModifiers(Modifier.PUBLIC)
                .returns(boolean.class)
                .addParameter(Object.class, "o")
                .addAnnotation(Override.class);

        equalsBuilder.beginControlFlow("if (this == o)")
                .addStatement("return true")
                .endControlFlow();

        equalsBuilder.beginControlFlow("if (!(o instanceof $T))", name)
                .addStatement("return false")
                .endControlFlow();

        equalsBuilder.addStatement("$T that = ($T) o", name, name);


        if (recordConstructorSpec.fields().isEmpty()) {
            // If there are no fields, we can return true immediately
            equalsBuilder.addStatement("return true");
            return equalsBuilder.build();
        }
        equalsBuilder.addStatement(recordConstructorSpec.fields().stream()
                .map(f -> {
                    String fieldName = f.name();
                    return CodeBlock.of("$T.equals(this.$L, that.$L)", Objects.class, fieldName, fieldName);
                })
                .collect(CodeBlock.joining(" && ", "return ", "")));

        return equalsBuilder.build();
    }

    public static MethodSpec genHashCode(RecordConstructorSpec recordConstructorSpec) {
        MethodSpec.Builder hashCodeBuilder = MethodSpec.methodBuilder("hashCode")
                .addModifiers(Modifier.PUBLIC)
                .returns(int.class)
                .addAnnotation(Override.class);

        if (recordConstructorSpec.fields().isEmpty()) {
            // If there are no fields, use identity hash code
            hashCodeBuilder.addStatement("return $T.identityHashCode(this)", System.class);
            return hashCodeBuilder.build();
        }

        CodeBlock hashCodeExpression = recordConstructorSpec.fields().stream()
                .map(f -> CodeBlock.of("this." + f.name(), Objects.class))
                .collect(CodeBlock.joining(", "));

        hashCodeBuilder.addStatement("return $T.hash($L)", Objects.class, hashCodeExpression);
        return hashCodeBuilder.build();
    }

    public static MethodSpec genToString(RecordConstructorSpec recordConstructorSpec, ClassName name) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("toString")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class);
        var code = CodeBlock.builder();
        code.add("return \"$T{\"", name);

        var properties = recordConstructorSpec.fields();

        code.add(properties.stream()
                .map(property -> CodeBlock.of("""
                        + "$L=" + $N\s""", property.name(), property.name()))
                .collect(CodeBlock.joining("""
                        + ", \"""")));

        code.add("+ \"}\"");
        builder.addStatement(code.build());
        return builder.build();
    }
}
