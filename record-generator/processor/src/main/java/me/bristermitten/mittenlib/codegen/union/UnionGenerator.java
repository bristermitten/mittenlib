package me.bristermitten.mittenlib.codegen.union;

import com.squareup.javapoet.*;
import me.bristermitten.mittenlib.codegen.GeneratedUnion;
import me.bristermitten.mittenlib.codegen.record.RecordConstructorSpec;
import me.bristermitten.mittenlib.codegen.record.RecordGenerator;

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


    private static ResolvedUnionSpec resolve(UnionSpec unionSpec) {
        return new ResolvedUnionSpec(
                unionSpec.source(),
                unionSpec.name(),
                unionSpec.strategy(),
                unionSpec.constructors().stream()
                        .map(constructor -> new ResolvedUnionConstructor(
                                unionSpec.source(),
                                unionSpec.name().nestedClass(constructor.name()),
                                constructor
                        ))
                        .toList()
        );
    }

    public JavaFile generate(UnionSpec unionSpec) {
        ClassName recordImplName = unionSpec.name();
        var typeSpecBuilder = TypeSpec.classBuilder(recordImplName);
        typeSpecBuilder.addModifiers(PUBLIC, ABSTRACT);
        typeSpecBuilder.addAnnotation(AnnotationSpec.builder(GeneratedUnion.class)
                .addMember("source", "$T.class", unionSpec.source())
                .build());

        var resolved = resolve(unionSpec);

        typeSpecBuilder.addMethod(MatchGenerator.makeVoidMatchMethodSpec(resolved));
        typeSpecBuilder.addMethod(MatchGenerator.makeMatchMethodSpec(resolved));


        for (var toGenerate : resolved.constructors()) {
            var generatedRecord = RecordGenerator.generateBasicRecordTypeSpec(toGenerate);

            var constructorTypeSpecBuilder =
                    generatedRecord.typeSpecBuilder()
                            .addModifiers(PUBLIC, STATIC, FINAL);

            constructorTypeSpecBuilder.superclass(recordImplName);

            RecordGenerator.addStaticFactoryMethod(toGenerate.constructor(), toGenerate.name(), typeSpecBuilder);
            generateAsMethod(toGenerate.name(), toGenerate.constructor(), typeSpecBuilder);

            constructorTypeSpecBuilder.addMethod(MatchGenerator.implementVoidMatchMethod(resolved, toGenerate));
            constructorTypeSpecBuilder.addMethod(MatchGenerator.implementReturningMatchMethod(resolved, toGenerate));


            typeSpecBuilder.addType(constructorTypeSpecBuilder.build());
        }

        addSealingConstructor(typeSpecBuilder);

        return JavaFile.builder(recordImplName.packageName(), typeSpecBuilder.build())
                .build();
    }


}
