package me.bristermitten.mittenlib.codegen.union;

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeVariableName;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import me.bristermitten.mittenlib.codegen.MatchStrategies;
import me.bristermitten.mittenlib.codegen.record.RecordConstructorSpec;

public class MatchGenerator {
    public static MethodSpec makeVoidMatchMethodSpec(ResolvedUnionSpec spec, boolean makeAbstract) {
        var builder = MethodSpec.methodBuilder("match")
                .addModifiers(PUBLIC)
                .returns(TypeName.get(void.class))
                .addParameters(spec.constructors().stream()
                        .map(constructor -> ParameterSpec.builder(
                                        voidFunctionalInterfaceFor(constructor, spec.strategy()),
                                        constructor.name().simpleName())
                                .build())
                        .toList());

        if (makeAbstract) builder.addModifiers(ABSTRACT);
        return builder.build();
    }

    public static MethodSpec makeMatchMethodSpec(ResolvedUnionSpec spec, boolean makeAbstract) {
        var builder = MethodSpec.methodBuilder("matchTo")
                .addModifiers(PUBLIC)
                .addTypeVariable(TypeVariableName.get("T"))
                .returns(TypeVariableName.get("T"))
                .addParameters(spec.constructors().stream()
                        .map(constructor -> ParameterSpec.builder(
                                        returningFunctionalInterfaceFor(
                                                constructor, spec.strategy(), TypeVariableName.get("T")),
                                        constructor.name().simpleName())
                                .build())
                        .toList());
        if (makeAbstract) builder.addModifiers(ABSTRACT);
        return builder.build();
    }

    public static MethodSpec implementVoidMatchMethod(ResolvedUnionSpec record, ResolvedUnionConstructor spec) {
        TypeName usedFunctionalInterface = voidFunctionalInterfaceFor(spec, record.strategy());
        String functionalInterfaceInvokeName = functionalInterfaceInvokeName(usedFunctionalInterface);
        var m = makeVoidMatchMethodSpec(record, false).toBuilder()
                .addAnnotation(Override.class)
                .addCode(CodeBlock.builder()
                        .add("$L.$L", spec.name().simpleName(), functionalInterfaceInvokeName)
                        .addStatement(matchParameters(record.strategy(), spec.constructor()))
                        .build());
        return m.build();
    }

    private static CodeBlock matchParameters(MatchStrategies strategy, RecordConstructorSpec constructor) {
        return switch (strategy) {
            case NOMINAL -> CodeBlock.of("(this)");
            case STRUCTURAL ->
                constructor.fields().stream()
                        .map(field -> CodeBlock.of("this.$L", field.name()))
                        .collect(CodeBlock.joining(", ", "(", ")"));
        };
    }

    public static MethodSpec implementReturningMatchMethod(ResolvedUnionSpec record, ResolvedUnionConstructor spec) {
        TypeName usedFunctionalInterface =
                returningFunctionalInterfaceFor(spec, record.strategy(), TypeVariableName.get("T"));
        String invokeName = functionalInterfaceInvokeName(usedFunctionalInterface);
        return makeMatchMethodSpec(record, false).toBuilder()
                .addAnnotation(Override.class)
                .returns(TypeVariableName.get("T"))
                .addCode(CodeBlock.builder()
                        .add("return $L.$L", spec.name().simpleName(), invokeName)
                        .addStatement(matchParameters(record.strategy(), spec.constructor()))
                        .build())
                .build();
    }

    public static TypeName voidFunctionalInterfaceFor(
            ResolvedUnionConstructor constructor, MatchStrategies strategies) {
        if (strategies == MatchStrategies.NOMINAL) {
            return ParameterizedTypeName.get(ClassName.get(Consumer.class), constructor.name());
        }
        var fields = constructor.constructor().fields();
        return switch (fields.size()) {
            case 0 -> ClassName.get(Runnable.class);

            case 1 ->
                switch (fields.getFirst().type()) {
                    case TypeName i
                    when !i.isPrimitive() -> ParameterizedTypeName.get(ClassName.get(Consumer.class), i);
                    case TypeName i when i.equals(TypeName.INT) -> ClassName.get(IntConsumer.class);
                    default ->
                        throw new UnsupportedOperationException("Unsupported type for match method with single field: "
                                + fields.getFirst().type());
                };
            case 2 ->
                ParameterizedTypeName.get(
                        ClassName.get(BiConsumer.class),
                        fields.getFirst().type().box(),
                        fields.get(1).type().box());
            default ->
                throw new UnsupportedOperationException(
                        "Unsupported number of fields for match method: " + fields.size());
        };
    }

    public static TypeName returningFunctionalInterfaceFor(
            ResolvedUnionConstructor spec, MatchStrategies strategies, TypeName returning) {
        return switch (strategies) {
            case NOMINAL -> ParameterizedTypeName.get(ClassName.get(Function.class), spec.name(), returning);
            case STRUCTURAL -> {
                var fields = spec.constructor().fields();
                yield switch (fields.size()) {
                    case 0 -> ParameterizedTypeName.get(ClassName.get(Supplier.class), returning);
                    case 1 ->
                        switch (fields.getFirst().type()) {
                            case TypeName i
                            when !i.isPrimitive() ->
                                ParameterizedTypeName.get(ClassName.get(Function.class), i, returning);
                            case TypeName i
                            when i.equals(TypeName.INT) ->
                                ParameterizedTypeName.get(ClassName.get(IntFunction.class), returning);
                            default ->
                                throw new UnsupportedOperationException(
                                        "Unsupported type for match method with single field: "
                                                + fields.getFirst().type());
                        };
                    case 2 ->
                        ParameterizedTypeName.get(
                                ClassName.get(BiFunction.class),
                                fields.getFirst().type().box(),
                                fields.get(1).type().box(),
                                returning);
                    default ->
                        throw new UnsupportedOperationException(
                                "Unsupported number of fields for match method: " + fields.size());
                };
            }
        };
    }

    private static String functionalInterfaceInvokeName(TypeName fi) {
        return switch (fi) {
            case ClassName c when c.equals(ClassName.get(Runnable.class)) -> "run";
            case ParameterizedTypeName p when p.rawType().equals(ClassName.get(Consumer.class)) -> "accept";
            case ParameterizedTypeName p when p.rawType().equals(ClassName.get(Function.class)) -> "apply";
            case ParameterizedTypeName p when p.rawType().equals(ClassName.get(IntFunction.class)) -> "apply";
            case ParameterizedTypeName p when p.rawType().equals(ClassName.get(Supplier.class)) -> "get";
            case ClassName c when c.equals(ClassName.get(IntConsumer.class)) -> "accept";
            case ParameterizedTypeName p when p.rawType().equals(ClassName.get(BiConsumer.class)) -> "accept";
            case ParameterizedTypeName p when p.rawType().equals(ClassName.get(BiFunction.class)) -> "apply";
            default -> throw new UnsupportedOperationException("Unsupported functional interface: " + fi);
        };
    }
}
