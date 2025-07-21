package me.bristermitten.mittenlib.records;

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

import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;


@SupportedSourceVersion(SourceVersion.RELEASE_21)
@AutoService(Processor.class)
public class RecordProcessor extends AbstractAnnotationProcessor {
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return createSupportedAnnotationSet(Record.class);
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


        var records = new ArrayList<RecordSpec>();
        // Process each annotated record class
        for (Element element : roundEnv.getElementsAnnotatedWith(Record.class)) {
            ElementWrapper<Element> wrap = ElementWrapper.wrap(element);
            wrap
                    .validateWithFluentElementValidator()
                    .is(AptkCoreMatchers.IS_INTERFACE)
                    .validateAndIssueMessages();

            TypeElementWrapper typeElement = TypeElementWrapper.toTypeElement(wrap);
            var constructors = new ArrayList<RecordSpec.RecordConstructorSpec>();
            for (ExecutableElement method : typeElement.filterEnclosedElements()
                    .applyFilter(AptkCoreMatchers.IS_METHOD)
                    .getResult()) {

                if (!TypeUtils.TypeComparison.isTypeEqual(
                        method.getReturnType(),
                        typeElement.asType().unwrap()
                )) {
                    MessagerUtils.error(method, RecordProcessorCompilerMessages.METHOD_BAD_RETURN, typeElement);
                    continue;
                }
                String constructorName = method.getSimpleName().toString();
                if (constructors.stream().anyMatch(
                        con -> con.name().equals(constructorName)
                )) {
                    MessagerUtils.error(method, RecordProcessorCompilerMessages.DUPLICATE_CONSTRUCTOR, constructorName);
                    continue; // Skip this constructor if it is a duplicate
                }
                constructors.add(
                        new RecordSpec.RecordConstructorSpec(
                                constructorName,
                                method.getParameters()
                                        .stream()
                                        .map(param ->
                                                new RecordSpec.RecordConstructorSpec.RecordFieldSpec(
                                                        param.getSimpleName().toString(),
                                                        TypeName.get(param.asType())
                                                ))
                                        .toList())
                );
            }


            ClassName recordSpecName = ClassName.get(typeElement.unwrap());

            recordSpecName = recordSpecName.peerClass(
                    recordSpecName.simpleName().replace("Spec", "")
            );

            var recordSpec = new RecordSpec(
                    ClassName.get(typeElement.unwrap()),
                    recordSpecName,
                    constructors
            );
            records.add(recordSpec);
        }

        if (records.isEmpty()) {
            return false; // No records to process
        }

        RecordGenerator generator = new RecordGenerator();
        for (RecordSpec record : records) {
            var generate = generator.generate(record);
            try {
                MessagerUtils.info(null, generate.toString());
                generate.writeTo(processingEnv.getFiler());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return true; // Indicate that annotations were processed
    }
}
