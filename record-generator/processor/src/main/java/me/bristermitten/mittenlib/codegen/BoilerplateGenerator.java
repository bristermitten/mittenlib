package me.bristermitten.mittenlib.codegen;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import me.bristermitten.mittenlib.codegen.record.RecordConstructorSpec;

import javax.lang.model.element.Modifier;
import java.util.Arrays;
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
                .map(BoilerplateGenerator::equalsCall)
                .collect(CodeBlock.joining(" && ", "return ", "")));

        return equalsBuilder.build();
    }

    private static CodeBlock equalsCall(RecordConstructorSpec.RecordFieldSpec fieldSpec) {
        String name = fieldSpec.name();
        if (fieldSpec.type() instanceof ArrayTypeName) {
            return CodeBlock.of("$T.equals(this.$L, that.$L)", Arrays.class, name, name);
        }
        return CodeBlock.of("$T.equals(this.$L, that.$L)", Objects.class, name, name);
    }

    private static CodeBlock hashCodeCall(RecordConstructorSpec.RecordFieldSpec fieldSpec) {
        String name = fieldSpec.name();
        if (fieldSpec.type() instanceof ArrayTypeName) {
            return CodeBlock.of("$T.hashCode(this.$L)", Arrays.class, name);
        }
        return CodeBlock.of("$T.hashCode(this.$L)", Objects.class, name);
    }

    private static CodeBlock toStringCall(RecordConstructorSpec.RecordFieldSpec fieldSpec) {
        String name = fieldSpec.name();
        if (fieldSpec.type() instanceof ArrayTypeName) {
            return CodeBlock.of("$T.toString(this.$L)", Arrays.class, name);
        }
        return CodeBlock.of("this.$L", name);
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
                .map(BoilerplateGenerator::hashCodeCall)
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
                        + "$L=" + $L\s""", property.name(), toStringCall(property)))
                .collect(CodeBlock.joining("""
                        + ", \"""")));

        code.add("+ \"}\"");
        builder.addStatement(code.build());
        return builder.build();
    }
}
