package me.bristermitten.mittenlib.codegen.record;

import com.squareup.javapoet.*;
import me.bristermitten.mittenlib.codegen.BoilerplateGenerator;
import org.jetbrains.annotations.NotNull;

import static javax.lang.model.element.Modifier.*;

public class RecordGenerator {
    public static void addAllArgsConstructor(RecordConstructorSpec constructor, TypeSpec.Builder constructorTypeSpecBuilder) {
        constructorTypeSpecBuilder.addMethod(
                MethodSpec.constructorBuilder()
                        .addModifiers(PUBLIC)
                        .addParameters(
                                constructor.fields().stream().map(
                                        field -> ParameterSpec.builder(field.type(), field.name()).build()
                                ).toList()
                        )
                        .addCode(
                                constructor.fields().stream()
                                        .map(field -> "this." + field.name() + " = " + field.name() + ";")
                                        .map(CodeBlock::of)
                                        .collect(CodeBlock.joining("\n"))
                        )
                        .build()
        );
    }

    public static void addFieldAndGetter(RecordConstructorSpec.RecordFieldSpec field, TypeSpec.Builder constructorTypeSpecBuilder) {
        constructorTypeSpecBuilder.addField(
                field.type(),
                field.name(),
                PRIVATE, FINAL
        );

        constructorTypeSpecBuilder.addMethod(
                MethodSpec.methodBuilder(field.name())
                        .addModifiers(PUBLIC)
                        .returns(field.type())
                        .addStatement("return this.$N", field.name())
                        .build()
        );
    }

    public static void addWithMethod(RecordConstructorSpec constructorSpec, RecordConstructorSpec.RecordFieldSpec field, ClassName returnTypeName, TypeSpec.Builder typeSpecBuilder) {
        typeSpecBuilder.addMethod(
                MethodSpec.methodBuilder("with" + field.name().substring(0, 1).toUpperCase() + field.name().substring(1))
                        .addModifiers(PUBLIC)
                        .returns(returnTypeName)
                        .addParameter(field.type(), field.name())
                        .addStatement("return new $T($L)", returnTypeName,
                                constructorSpec.fields().stream()
                                        .map(f -> f.name().equals(field.name())
                                                ? CodeBlock.of(field.name())
                                                : CodeBlock.of("this.$L", f.name()) // Use existing field value
                                        )
                                        .collect(CodeBlock.joining(", ")
                                        ))
                        .build()
        );
    }

    public static void addStaticFactoryMethod(RecordConstructorSpec constructor, ClassName returnTypeName, TypeSpec.Builder typeSpecBuilder) {
        typeSpecBuilder.addMethod(
                MethodSpec.methodBuilder(constructor.name())
                        .addModifiers(PUBLIC, STATIC)
                        .returns(returnTypeName)
                        .addParameters(
                                constructor.fields().stream()
                                        .map(field -> ParameterSpec.builder(field.type(), field.name()).build())
                                        .toList()
                        )
                        .addStatement("return new $L($L)", returnTypeName, constructor.fields().stream()
                                .map(RecordConstructorSpec.RecordFieldSpec::name)
                                .map(CodeBlock::of)
                                .collect(CodeBlock.joining(", ")))
                        .build()
        );
    }

    public static @NotNull RecordGenerator.GeneratedRecord generateBasicRecordTypeSpec(RecordSpecLike record) {
        ClassName recordImplName = record.name();

        var typeSpecBuilder = TypeSpec.classBuilder(recordImplName);
        typeSpecBuilder.addModifiers(PUBLIC, FINAL);
        typeSpecBuilder.addAnnotation(AnnotationSpec.builder(me.bristermitten.mittenlib.codegen.GeneratedRecord.class)
                .addMember("source", "$T.class", record.source())
                .build());

        for (var field : record.constructor().fields()) {
            addFieldAndGetter(field, typeSpecBuilder);
            addWithMethod(record.constructor(), field, recordImplName, typeSpecBuilder);
        }
        addAllArgsConstructor(record.constructor(), typeSpecBuilder);


        // Add equals, hashCode, and toString methods
        typeSpecBuilder.addMethod(
                BoilerplateGenerator.genToString(record.constructor(), recordImplName));
        typeSpecBuilder.addMethod(
                BoilerplateGenerator.genEquals(record.constructor(), recordImplName));
        typeSpecBuilder.addMethod(
                BoilerplateGenerator.genHashCode(record.constructor()));
        return new GeneratedRecord(recordImplName, typeSpecBuilder);
    }

    public JavaFile generate(RecordSpec record) {
        GeneratedRecord result = generateBasicRecordTypeSpec(record);
        addStaticFactoryMethod(record.constructor(), record.name(), result.typeSpecBuilder());

        return JavaFile.builder(result.recordImplName().packageName(), result.typeSpecBuilder().build())
                .build();
    }

    public record GeneratedRecord(ClassName recordImplName, TypeSpec.Builder typeSpecBuilder) {
    }
}
