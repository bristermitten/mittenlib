package me.bristermitten.mittenlib.codegen.record;

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
import me.bristermitten.mittenlib.codegen.Record;
import me.bristermitten.mittenlib.codegen.RecordProcessorCompilerMessages;
import me.bristermitten.mittenlib.codegen.Union;
import me.bristermitten.mittenlib.codegen.union.UnionGenerator;
import me.bristermitten.mittenlib.codegen.union.UnionSpec;

import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.util.*;


@SupportedSourceVersion(SourceVersion.RELEASE_21)
@AutoService(Processor.class)
public class RecordProcessor extends AbstractAnnotationProcessor {
    private static Optional<RecordConstructorSpec> parseConstructor(ExecutableElement method, TypeElementWrapper typeElement, Collection<String> existingConstructors) {
        if (!TypeUtils.TypeComparison.isTypeEqual(
                method.getReturnType(),
                typeElement.asType().unwrap()
        )) {
            MessagerUtils.error(method, RecordProcessorCompilerMessages.METHOD_BAD_RETURN, typeElement);
            return Optional.empty();
        }
        String constructorName = method.getSimpleName().toString();
        if (existingConstructors.stream().anyMatch(
                con -> con.equals(constructorName)
        )) {
            MessagerUtils.error(method, RecordProcessorCompilerMessages.DUPLICATE_CONSTRUCTOR, constructorName);
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
        var recordSpecName = ClassName.get(spec);
        return ClassName.get(
                recordSpecName.packageName(),
                recordSpecName.simpleName().replace("Spec", "")
        );
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return createSupportedAnnotationSet(me.bristermitten.mittenlib.codegen.Record.class, Union.class);
    }

    @Override
    @DeclareCompilerMessage(code = "001", enumValueName = "METHOD_BAD_RETURN",
            message = "Method must return the record type ${0}!"
    )
    @DeclareCompilerMessage(code = "002", enumValueName = "DUPLICATE_CONSTRUCTOR",
            message = "Constructors must have distinct names, overloading is not allowed"
    )
    public boolean processAnnotations(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false; // No further processing needed
        }


        var unions = processUnions(roundEnv);
        var records = processRecords(roundEnv);

        return unions && records;

    }

    @DeclareCompilerMessage(code = "003", enumValueName = "RECORD_MUST_HAVE_SINGLE_CONSTRUCTOR",
            message = "Record ${0} must have a single constructor!"
    )
    private boolean processRecords(RoundEnvironment roundEnv) {
        var records = new ArrayList<RecordSpec>();

        // Process each annotated record class
        for (Element element : roundEnv.getElementsAnnotatedWith(Record.class)) {
            ElementWrapper<Element> wrap = ElementWrapper.wrap(element);
            wrap
                    .validateWithFluentElementValidator()
                    .is(AptkCoreMatchers.IS_INTERFACE)
                    .validateAndIssueMessages();

            TypeElementWrapper typeElement = TypeElementWrapper.toTypeElement(wrap);

            List<ExecutableElement> result = typeElement.filterEnclosedElements()
                    .applyFilter(AptkCoreMatchers.IS_METHOD)
                    .getResult();
            if (result.size() != 1) {
                MessagerUtils.error(typeElement.unwrap(), RecordProcessorCompilerMessages.RECORD_MUST_HAVE_SINGLE_CONSTRUCTOR, typeElement.unwrap());
                continue; // Skip invalid records
            }

            ExecutableElement method = result.getFirst();
            Optional<RecordConstructorSpec> recordConstructorSpec = parseConstructor(method, typeElement, List.of());
            if (recordConstructorSpec.isEmpty()) {
                MessagerUtils.error(method, RecordProcessorCompilerMessages.RECORD_MUST_HAVE_SINGLE_CONSTRUCTOR, typeElement);
                return false; // Skip invalid records
            }
            RecordConstructorSpec constructor = recordConstructorSpec.get();

            ClassName recordSpecName = getSpecName(typeElement.unwrap());

            var recordSpec = new RecordSpec(
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
                MessagerUtils.info(null, generate.toString());
                generate.writeTo(processingEnv.getFiler());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


        return true;
    }

    private boolean processUnions(RoundEnvironment roundEnv) {
        var unions = new ArrayList<UnionSpec>();
        // Process each annotated record class
        for (Element element : roundEnv.getElementsAnnotatedWith(Union.class)) {
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

                Optional<RecordConstructorSpec> recordConstructorSpec = parseConstructor(method, typeElement, constructors.stream().map(
                        RecordConstructorSpec::name
                ).toList());

                if (recordConstructorSpec.isEmpty()) {
                    continue; // Skip invalid constructors
                }
                constructors.add(recordConstructorSpec.get());
            }


            ClassName recordSpecName = getSpecName(typeElement.unwrap());

            var recordSpec = new UnionSpec(
                    ClassName.get(typeElement.unwrap()),
                    recordSpecName,
                    constructors
            );
            unions.add(recordSpec);
        }

        if (unions.isEmpty()) {
            return false;
        }

        UnionGenerator generator = new UnionGenerator();
        for (UnionSpec record : unions) {
            var generate = generator.generate(record);
            try {
                MessagerUtils.info(null, generate.toString());
                generate.writeTo(processingEnv.getFiler());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return true;
    }
}
