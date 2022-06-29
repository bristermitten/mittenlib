package me.bristermitten.mittenlib.annotations.config;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.JavaFile;
import me.bristermitten.mittenlib.annotations.util.ElementsFinder;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.groupingBy;

@SupportedAnnotationTypes("me.bristermitten.mittenlib.config.Config")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
public class ConfigProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        var elementsFinder = new ElementsFinder(processingEnv.getTypeUtils());
        final Map<Element, List<VariableElement>> types = annotations
                .stream()
                .map(roundEnv::getElementsAnnotatedWith)
                .flatMap(Collection::stream)
                .filter(TypeElement.class::isInstance)
                .map(TypeElement.class::cast)
                .filter(element -> element.getNestingKind() == NestingKind.TOP_LEVEL)
                .map(elementsFinder::getApplicableVariableElements)
                .flatMap(Collection::stream)
                .collect(groupingBy(VariableElement::getEnclosingElement));

        ConfigClassBuilder builder = new ConfigClassBuilder(processingEnv, elementsFinder);
        types.forEach((clazz, fields) -> {
            JavaFile fileContent = builder.createConfigFile((TypeElement) clazz, fields);
            try {
                fileContent.writeTo(processingEnv.getFiler());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return true;
    }
}
