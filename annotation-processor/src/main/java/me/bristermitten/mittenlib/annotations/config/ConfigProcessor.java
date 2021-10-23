package me.bristermitten.mittenlib.annotations.config;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.JavaFile;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.groupingBy;

@SupportedAnnotationTypes("me.bristermitten.mittenlib.config.Config")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class ConfigProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        final Map<Element, List<VariableElement>> types = annotations
                .stream()
                .map(roundEnv::getElementsAnnotatedWith)
                .flatMap(Collection::stream)
                .map(Element::getEnclosedElements)
                .flatMap(Collection::stream)
                .filter(element -> element.getKind().isField())
                .map(VariableElement.class::cast)
                .filter(elem -> !elem.getModifiers().contains(Modifier.TRANSIENT)) // ignore transient fields
                .filter(elem -> !elem.getModifiers().contains(Modifier.STATIC)) // ignore static fields
                .collect(groupingBy(VariableElement::getEnclosingElement));

        ConfigClassBuilder builder = new ConfigClassBuilder(processingEnv);
        types.forEach((clazz, fields) -> {
            JavaFile fileContent = builder.createConfigClass((TypeElement) clazz, fields);
            try {
                fileContent.writeTo(processingEnv.getFiler());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return true;
    }
}
