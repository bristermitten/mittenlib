package me.bristermitten.mittenlib.annotations.config;

import com.google.auto.service.AutoService;
import com.google.inject.Guice;
import com.squareup.javapoet.JavaFile;
import me.bristermitten.mittenlib.config.Config;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Annotation processor for generating config classes from DTO classes marked with {@link Config}
 */
@SupportedAnnotationTypes("me.bristermitten.mittenlib.config.Config")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
public class ConfigProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        var injector = Guice.createInjector(
                new ConfigProcessorModule(processingEnv)
        );

        final List<TypeElement> types = annotations
                .stream()
                .map(roundEnv::getElementsAnnotatedWith)
                .flatMap(Collection::stream)
                .filter(TypeElement.class::isInstance)
                .map(TypeElement.class::cast)
                .filter(element -> element.getNestingKind() == NestingKind.TOP_LEVEL)
                .toList();

        ConfigClassBuilder builder = injector.getInstance(ConfigClassBuilder.class);
        types.forEach(clazz -> {
            JavaFile fileContent = builder.createConfigFile(clazz);
            try {
                fileContent.writeTo(processingEnv.getFiler());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return true;
    }
}
