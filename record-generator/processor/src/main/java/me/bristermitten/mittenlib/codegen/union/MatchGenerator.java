package me.bristermitten.mittenlib.codegen.union;

import com.squareup.javapoet.*;
import me.bristermitten.mittenlib.codegen.record.RecordConstructorSpec;

import java.util.function.*;

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;

public class MatchGenerator {
    public static MethodSpec makeVoidMatchMethodSpec(UnionSpec spec) {
        return MethodSpec.methodBuilder("match")
                .addModifiers(PUBLIC, ABSTRACT)
                .returns(TypeName.get(void.class))
                .addParameters(
                        spec.constructors()
                                .stream().map(
                                        constructor -> ParameterSpec.builder(
                                                voidFunctionalInterfaceFor(constructor),
                                                constructor.name()
                                        ).build()
                                )
                                .toList()
                )
                .build();
    }

    public static MethodSpec makeMatchMethodSpec(UnionSpec spec) {
        return MethodSpec.methodBuilder("matchTo")
                .addModifiers(PUBLIC, ABSTRACT)
                .addTypeVariable(TypeVariableName.get("T"))
                .returns(TypeVariableName.get("T"))
                .addParameters(
                        spec.constructors()
                                .stream().map(
                                        constructor -> ParameterSpec.builder(
                                                returningFunctionalInterfaceFor(constructor, TypeVariableName.get("T")),
                                                constructor.name()
                                        ).build()
                                )
                                .toList()
                )
                .build();
    }

    public static MethodSpec implementVoidMatchMethod(UnionSpec record, RecordConstructorSpec spec) {
        var m = makeVoidMatchMethodSpec(record)
                .toBuilder()
                .addAnnotation(Override.class)
                .addCode(
                        CodeBlock.builder().add("$L.$L", spec.name(), functionalInterfaceInvokeName(voidFunctionalInterfaceFor(spec)))
                                .addStatement(
                                        spec.fields().stream()
                                                .map(field -> CodeBlock.of("this.$L", field.name()))
                                                .collect(CodeBlock.joining(", ", "(", ")"))
                                )
                                .build()
                );
        m.modifiers.remove(ABSTRACT);
        return m.build();
    }

    public static MethodSpec implementReturningMatchMethod(UnionSpec record, RecordConstructorSpec spec) {
        var m = makeMatchMethodSpec(record)
                .toBuilder()
                .addAnnotation(Override.class)
                .returns(TypeVariableName.get("T"))
                .addCode(
                        CodeBlock.builder().add("return $L.$L", spec.name(), functionalInterfaceInvokeName(returningFunctionalInterfaceFor(spec, TypeVariableName.get("T"))))
                                .addStatement(
                                        spec.fields().stream()
                                                .map(field -> CodeBlock.of("this.$L", field.name()))
                                                .collect(CodeBlock.joining(", ", "(", ")"))
                                )
                                .build()
                );
        m.modifiers.remove(ABSTRACT);
        return m.build();
    }

    public static TypeName voidFunctionalInterfaceFor(RecordConstructorSpec spec) {
        return switch (spec.fields().size()) {
            case 0 -> ClassName.get(Runnable.class);

            case 1 -> switch (spec.fields().getFirst().type()) {
                case TypeName i when !i.isPrimitive() -> ParameterizedTypeName.get(
                        ClassName.get(Consumer.class),
                        i
                );
                case TypeName i when i.equals(TypeName.INT) -> ClassName.get(IntConsumer.class);
                default ->
                        throw new UnsupportedOperationException("Unsupported type for match method with single field: " + spec.fields().getFirst().type());
            };
            case 2 -> ParameterizedTypeName.get(
                    ClassName.get(BiConsumer.class),
                    spec.fields().getFirst().type().box(),
                    spec.fields().get(1).type().box()
            );
            default ->
                    throw new UnsupportedOperationException("Unsupported number of fields for match method: " + spec.fields().size());
        };
    }

    public static TypeName returningFunctionalInterfaceFor(RecordConstructorSpec spec, TypeName returning) {
        return switch (spec.fields().size()) {
            case 0 -> ParameterizedTypeName.get(
                    ClassName.get(Supplier.class),
                    returning
            );
            case 1 -> switch (spec.fields().getFirst().type()) {
                case TypeName i when !i.isPrimitive() -> ParameterizedTypeName.get(
                        ClassName.get(Function.class),
                        i,
                        returning
                );
                case TypeName i when i.equals(TypeName.INT) -> ParameterizedTypeName.get(
                        ClassName.get(IntFunction.class),
                        returning
                );
                default ->
                        throw new UnsupportedOperationException("Unsupported type for match method with single field: " + spec.fields().getFirst().type());
            };
            case 2 -> ParameterizedTypeName.get(
                    ClassName.get(BiFunction.class),
                    spec.fields().getFirst().type().box(),
                    spec.fields().get(1).type().box(),
                    returning
            );
            default ->
                    throw new UnsupportedOperationException("Unsupported number of fields for match method: " + spec.fields().size());
        };
    }

    private static String functionalInterfaceInvokeName(TypeName fi) {
        return switch (fi) {
            case ClassName c when c.equals(ClassName.get(Runnable.class)) -> "run";
            case ParameterizedTypeName p when p.rawType.equals(ClassName.get(Consumer.class)) -> "accept";
            case ParameterizedTypeName p when p.rawType.equals(ClassName.get(Function.class)) -> "apply";
            case ParameterizedTypeName p when p.rawType.equals(ClassName.get(IntFunction.class)) -> "apply";
            case ParameterizedTypeName p when p.rawType.equals(ClassName.get(Supplier.class)) -> "get";
            case ClassName c when c.equals(ClassName.get(IntConsumer.class)) -> "accept";
            case ParameterizedTypeName p when p.rawType.equals(ClassName.get(BiConsumer.class)) -> "accept";
            case ParameterizedTypeName p when p.rawType.equals(ClassName.get(BiFunction.class)) -> "apply";
            default -> throw new UnsupportedOperationException("Unsupported functional interface: " + fi);
        };
    }
}
