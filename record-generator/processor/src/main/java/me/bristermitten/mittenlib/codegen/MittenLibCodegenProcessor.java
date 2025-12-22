package me.bristermitten.mittenlib.codegen;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import io.toolisticon.aptk.compilermessage.api.DeclareCompilerMessage;
import io.toolisticon.aptk.tools.AbstractAnnotationProcessor;
import io.toolisticon.aptk.tools.MessagerUtils;
import io.toolisticon.aptk.tools.TypeUtils;
import io.toolisticon.aptk.tools.corematcher.AptkCoreMatchers;
import io.toolisticon.aptk.tools.wrapper.ElementWrapper;
import io.toolisticon.aptk.tools.wrapper.TypeElementWrapper;
import me.bristermitten.mittenlib.codegen.record.RecordConstructorSpec;
import me.bristermitten.mittenlib.codegen.record.RecordGenerator;
import me.bristermitten.mittenlib.codegen.union.UnionGenerator;

import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.util.*;


@SupportedSourceVersion(SourceVersion.RELEASE_21)
@AutoService(Processor.class)
@SupportedOptions("org.gradle.annotation.processing.isolating")
public class MittenLibCodegenProcessor extends AbstractAnnotationProcessor {

    private static Optional<RecordConstructorSpec> parseRecord(TypeElementWrapper typeElementWrapper) {
        List<ExecutableElement> getters = typeElementWrapper.filterEnclosedElements()
                .applyFilter(AptkCoreMatchers.IS_METHOD)
                .applyFilter(AptkCoreMatchers.HAS_NO_PARAMETERS)
                .getResult();


        var fields = getters
                .stream()
                .map(method -> new RecordConstructorSpec.RecordFieldSpec(
                        method.getSimpleName().toString(),
                        TypeName.get(method.getReturnType())
                ))
                .toList();

        return Optional.of(
                new RecordConstructorSpec(
                        "create", // TODO: make customisable
                        fields
                )
        );


    }

    private static Optional<RecordConstructorSpec> parseConstructor(ExecutableElement method, TypeElementWrapper typeElement, Collection<String> existingConstructors) {
        if (!TypeUtils.TypeComparison.isTypeEqual(
                method.getReturnType(),
                typeElement.asType().unwrap()
        )) {
            MessagerUtils.error(method, MittenLibCodegenProcessorCompilerMessages.METHOD_BAD_RETURN, typeElement.unwrap());
            return Optional.empty();
        }
        String constructorName = method.getSimpleName().toString();
        if (existingConstructors.stream().anyMatch(
                con -> con.equals(constructorName)
        )) {
            MessagerUtils.error(method, MittenLibCodegenProcessorCompilerMessages.DUPLICATE_CONSTRUCTOR, constructorName);
            return Optional.empty();
        }
        return Optional.of(
                new RecordConstructorSpec(
                        constructorName,
                        method.getParameters()
                                .stream()
                                .map(param ->
                                        new RecordConstructorSpec.RecordFieldSpec(
                                                param.getSimpleName().toString(),
                                                TypeName.get(param.asType())
                                        ))
                                .toList())
        );
    }

    private static ClassName getSpecName(TypeElement spec) {
        TypeElementWrapper wrapped = TypeElementWrapper.wrap(spec);
        var explicitName = wrapped.getAnnotation(RecordSpec.class)
                .map(RecordSpec::name)
                .or(() -> wrapped.getAnnotation(UnionSpec.class).map(UnionSpec::name))
                .filter(name -> !name.isBlank());

        var recordSpecName = ClassName.get(spec);
        if (explicitName.isPresent()) {
            return ClassName.get(
                    recordSpecName.packageName(),
                    explicitName.get()
            );
        }

        return ClassName.get(
                recordSpecName.packageName(),
                recordSpecName.simpleName().replace("Spec", "")
        );
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return createSupportedAnnotationSet(RecordSpec.class, UnionSpec.class);
    }

    @Override
    @DeclareCompilerMessage(code = "001", enumValueName = "METHOD_BAD_RETURN",
            message = "Method must return the record type ${0}!"
    )
    @DeclareCompilerMessage(code = "002", enumValueName = "DUPLICATE_CONSTRUCTOR",
            message = "Constructors must have distinct names, overloading is not allowed"
    )
    public boolean processAnnotations(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {


        var unions = processUnions(roundEnv);
        var records = processRecords(roundEnv);

        return unions && records;

    }

    @DeclareCompilerMessage(code = "003", enumValueName = "INVALID_RECORD",
            message = "Could not parse record ${0}."
    )
    private boolean processRecords(RoundEnvironment roundEnv) {
        var records = new ArrayList<me.bristermitten.mittenlib.codegen.record.RecordSpec>();

        // Process each annotated record class
        for (Element element : roundEnv.getElementsAnnotatedWith(RecordSpec.class)) {
            ElementWrapper<Element> wrap = ElementWrapper.wrap(element);
            wrap
                    .validateWithFluentElementValidator()
                    .is(AptkCoreMatchers.IS_INTERFACE)
                    .validateAndIssueMessages();

            TypeElementWrapper typeElement = TypeElementWrapper.toTypeElement(wrap);

            Optional<RecordConstructorSpec> recordConstructorSpec = parseRecord(typeElement);
            if (recordConstructorSpec.isEmpty()) {
                MessagerUtils.error(element, MittenLibCodegenProcessorCompilerMessages.INVALID_RECORD, typeElement);
                return false; // Skip invalid records
            }
            RecordConstructorSpec constructor = recordConstructorSpec.get();

            ClassName recordSpecName = getSpecName(typeElement.unwrap());

            var recordSpec = new me.bristermitten.mittenlib.codegen.record.RecordSpec(
                    ClassName.get(typeElement.unwrap()),
                    recordSpecName,
                    constructor
            );
            records.add(recordSpec);
        }

        RecordGenerator generator = new RecordGenerator();
        for (var record : records) {
            var generate = generator.generate(record);
            try {
                generate.writeTo(processingEnv.getFiler());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


        return true;
    }

    private boolean processUnions(RoundEnvironment roundEnv) {
        var unions = new ArrayList<me.bristermitten.mittenlib.codegen.union.UnionSpec>();
        // Process each annotated record class
        for (Element element : roundEnv.getElementsAnnotatedWith(UnionSpec.class)) {
            ElementWrapper<Element> wrap = ElementWrapper.wrap(element);
            wrap
                    .validateWithFluentElementValidator()
                    .is(AptkCoreMatchers.IS_INTERFACE)
                    .validateAndIssueMessages();

            TypeElementWrapper typeElement = TypeElementWrapper.toTypeElement(wrap);
            var constructors = new ArrayList<RecordConstructorSpec>();
            for (ExecutableElement method : typeElement.filterEnclosedElements()
                    .applyFilter(AptkCoreMatchers.IS_METHOD)
                    .getResult()) {

                Optional<RecordConstructorSpec> recordConstructorSpec = parseConstructor(method, typeElement,
                        constructors.stream()
                                .map(RecordConstructorSpec::name)
                                .toList());

                if (recordConstructorSpec.isEmpty()) {
                    continue; // Skip invalid constructors
                }
                constructors.add(recordConstructorSpec.get());
            }


            ClassName recordSpecName = getSpecName(typeElement.unwrap());

            var matchStrategy = typeElement.getAnnotation(MatchStrategy.class)
                    .map(MatchStrategy::value)
                    .orElse(MatchStrategies.NOMINAL);

            var recordSpec = new me.bristermitten.mittenlib.codegen.union.UnionSpec(
                    ClassName.get(typeElement.unwrap()),
                    recordSpecName,
                    matchStrategy,
                    constructors
            );
            unions.add(recordSpec);
        }

        if (unions.isEmpty()) {
            return false;
        }

        UnionGenerator generator = new UnionGenerator();
        for (var union : unions) {
            var generate = generator.generate(union);
            try {
                generate.writeTo(processingEnv.getFiler());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return true;
    }
}
