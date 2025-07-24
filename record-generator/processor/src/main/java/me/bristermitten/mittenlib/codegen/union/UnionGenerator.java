package me.bristermitten.mittenlib.codegen.union;

import com.squareup.javapoet.*;
import me.bristermitten.mittenlib.codegen.GeneratedUnion;
import me.bristermitten.mittenlib.codegen.record.RecordConstructorSpec;
import me.bristermitten.mittenlib.codegen.record.RecordGenerator;
import me.bristermitten.mittenlib.codegen.record.RecordSpecLike;

import java.util.Optional;

import static javax.lang.model.element.Modifier.*;

public class UnionGenerator {

    private static void addSealingConstructor(TypeSpec.Builder typeSpecBuilder) {
        typeSpecBuilder.addMethod(
                MethodSpec.constructorBuilder()
                        .addModifiers(PRIVATE)
                        .addCode(
                                typeSpecBuilder.typeSpecs.stream().map(
                                                m -> CodeBlock.of("this instanceof $L", m.name)
                                        )
                                        .collect(CodeBlock.joining(" || ", "if (!(", """
                                                )) {
                                                    throw new UnsupportedOperationException("Union type is sealed!: " + this.getClass().getName());
                                                }
                                                """
                                        ))).build()
        );
    }


    private static void generateAsMethod(ClassName constructorClassName, RecordConstructorSpec constructor, TypeSpec.Builder typeSpecBuilder) {
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

    public JavaFile generate(UnionSpec unionSpec) {
        ClassName recordImplName = unionSpec.name();
        var typeSpecBuilder = TypeSpec.classBuilder(recordImplName);
        typeSpecBuilder.addModifiers(PUBLIC, ABSTRACT);
        typeSpecBuilder.addAnnotation(AnnotationSpec.builder(GeneratedUnion.class)
                .addMember("source", "$T.class", unionSpec.source())
                .build());


        typeSpecBuilder.addMethod(MatchGenerator.makeVoidMatchMethodSpec(unionSpec));
        typeSpecBuilder.addMethod(MatchGenerator.makeMatchMethodSpec(unionSpec));


        record RecordConstructorToGenerate(
                ClassName source, ClassName name, RecordConstructorSpec constructor
        ) implements RecordSpecLike {
        }
        for (RecordConstructorSpec constructor : unionSpec.constructors()) {
            ClassName constructorClassName = recordImplName.nestedClass(constructor.name());
            var toGenerate = new RecordConstructorToGenerate(
                    unionSpec.source(),
                    constructorClassName,
                    constructor
            );
            var generatedRecord = RecordGenerator.generateBasicRecordTypeSpec(toGenerate);

            var constructorTypeSpecBuilder =
                    generatedRecord.typeSpecBuilder()
                            .addModifiers(PUBLIC, STATIC, FINAL);

            constructorTypeSpecBuilder.superclass(recordImplName);

            RecordGenerator.addStaticFactoryMethod(constructor, constructorClassName, typeSpecBuilder);
            generateAsMethod(constructorClassName, constructor, typeSpecBuilder);

            constructorTypeSpecBuilder.addMethod(MatchGenerator.implementVoidMatchMethod(unionSpec, constructor));
            constructorTypeSpecBuilder.addMethod(MatchGenerator.implementReturningMatchMethod(unionSpec, constructor));


            typeSpecBuilder.addType(constructorTypeSpecBuilder.build());
        }

        addSealingConstructor(typeSpecBuilder);

        return JavaFile.builder(recordImplName.packageName(), typeSpecBuilder.build())
                .build();
    }


}
