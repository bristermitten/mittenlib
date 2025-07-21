package me.bristermitten.mittenlib.records;

import com.squareup.javapoet.*;

import java.util.Optional;

import static javax.lang.model.element.Modifier.*;

public class RecordGenerator {

    private static void addSafeConstructor(TypeSpec.Builder typeSpecBuilder) {
        typeSpecBuilder.addMethod(
                MethodSpec.constructorBuilder()
                        .addModifiers(PRIVATE)
                        .addCode(
                                typeSpecBuilder.typeSpecs.stream().map(
                                                m -> CodeBlock.of("this instanceof $L", m.name)
                                        )
                                        .collect(CodeBlock.joining(" || ", "if (!(", """
                                                )) {
                                                    throw new UnsupportedOperationException("Record type is sealed!: " + this.getClass().getName());
                                                }
                                                """
                                        ))).build()
        );
    }

    private static void addAllArgsConstructor(RecordSpec.RecordConstructorSpec constructor, TypeSpec.Builder constructorTypeSpecBuilder) {
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

    private static void addFieldAndGetter(RecordSpec.RecordConstructorSpec.RecordFieldSpec field, TypeSpec.Builder constructorTypeSpecBuilder) {
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

    private static void generateAsMethod(ClassName constructorClassName, RecordSpec.RecordConstructorSpec constructor, TypeSpec.Builder typeSpecBuilder) {
        typeSpecBuilder.addMethod(
                MethodSpec.methodBuilder("as" + constructor.name())
                        .addModifiers(PUBLIC)
                        .returns(ParameterizedTypeName.get(ClassName.get(Optional.class), constructorClassName))
                        .addCode(CodeBlock.builder()
                                .beginControlFlow("if (this instanceof $L)", constructorClassName)
                                .addStatement("return $T.of(($T) this)", Optional.class, constructorClassName)
                                .endControlFlow()
                                .addStatement("return $T.empty()", Optional.class)
                                .build()
                        )
                        .build()
        );
    }

    public JavaFile generate(RecordSpec recordSpec) {
        ClassName recordImplName = ClassName.get(recordSpec.name().packageName(), recordSpec.name().simpleName());
        var typeSpecBuilder = TypeSpec.classBuilder(recordImplName);
        typeSpecBuilder.addModifiers(PUBLIC, ABSTRACT);
        typeSpecBuilder.addAnnotation(AnnotationSpec.builder(GeneratedRecord.class)
                .addMember("source", "$T.class", recordSpec.source())
                .build());


        typeSpecBuilder.addMethod(MatchGenerator.makeVoidMatchMethodSpec(recordSpec));
        typeSpecBuilder.addMethod(MatchGenerator.makeMatchMethodSpec(recordSpec));


        for (RecordSpec.RecordConstructorSpec constructor : recordSpec.constructors()) {
            ClassName constructorClassName = recordImplName.nestedClass(constructor.name());
            var constructorTypeSpecBuilder = TypeSpec.classBuilder(constructorClassName)
                    .addModifiers(PUBLIC, STATIC, FINAL);

            constructorTypeSpecBuilder.superclass(recordImplName);

            addStaticFactoryMethod(constructor, constructorClassName, typeSpecBuilder);
            generateAsMethod(constructorClassName, constructor, typeSpecBuilder);

            constructorTypeSpecBuilder.addMethod(MatchGenerator.implementVoidMatchMethod(recordSpec, constructor));
            constructorTypeSpecBuilder.addMethod(MatchGenerator.implementReturningMatchMethod(recordSpec, constructor));

            for (RecordSpec.RecordConstructorSpec.RecordFieldSpec field : constructor.fields()) {
                addFieldAndGetter(field, constructorTypeSpecBuilder);
            }

            addAllArgsConstructor(constructor, constructorTypeSpecBuilder);

            constructorTypeSpecBuilder.addMethod(
                    BoilerplateGenerator.genEquals(constructor, constructorClassName)
            );

            constructorTypeSpecBuilder.addMethod(
                    BoilerplateGenerator.genHashCode(constructor)
            );

            constructorTypeSpecBuilder.addMethod(BoilerplateGenerator.genToString(constructor, constructorClassName));


            typeSpecBuilder.addType(constructorTypeSpecBuilder.build());
        }

        addSafeConstructor(typeSpecBuilder);

        return JavaFile.builder(recordImplName.packageName(), typeSpecBuilder.build())
                .build();
    }

    private void addStaticFactoryMethod(RecordSpec.RecordConstructorSpec constructor, ClassName constructorName, TypeSpec.Builder typeSpecBuilder) {
        typeSpecBuilder.addMethod(
                MethodSpec.methodBuilder(constructor.name())
                        .addModifiers(PUBLIC, STATIC)
                        .returns(constructorName)
                        .addParameters(
                                constructor.fields().stream()
                                        .map(field -> ParameterSpec.builder(field.type(), field.name()).build())
                                        .toList()
                        )
                        .addStatement("return new $L($L)", constructor.name(), constructor.fields().stream()
                                .map(RecordSpec.RecordConstructorSpec.RecordFieldSpec::name)
                                .map(CodeBlock::of)
                                .collect(CodeBlock.joining(", ")))
                        .build()
        );
    }


}
