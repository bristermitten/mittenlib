package me.bristermitten.mittenlib.annotations.config;

import com.google.auto.service.AutoService;
import com.google.inject.Guice;
import com.squareup.javapoet.JavaFile;
import io.toolisticon.aptk.common.ToolingProvider;
import io.toolisticon.aptk.tools.AbstractAnnotationProcessor;
import me.bristermitten.mittenlib.annotations.ast.AbstractConfigStructure;
import me.bristermitten.mittenlib.annotations.compile.ConfigImplGenerator;
import me.bristermitten.mittenlib.annotations.exception.ConfigProcessingException;
import me.bristermitten.mittenlib.annotations.parser.ConfigClassParser;
import me.bristermitten.mittenlib.config.Config;

import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Annotation processor for generating config classes from DTO classes marked with {@link Config}
 */
@SupportedAnnotationTypes("me.bristermitten.mittenlib.config.Config")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@AutoService(Processor.class)
public class ConfigProcessor extends AbstractAnnotationProcessor {

    /**
     * Public constructor for the compiler
     */
    public ConfigProcessor() {
        super();
    }

    @Override
    public boolean processAnnotations(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        ToolingProvider.setTooling(processingEnv);
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

        List<AbstractConfigStructure> asts = new ArrayList<>();
        var configClassParser = injector.getInstance(ConfigClassParser.class);
        for (TypeElement clazz : types) {
            var ast = configClassParser.parseAbstract(clazz);
            if (ast == null) {
                return false;
            }
            asts.add(ast);
        }

        var generator = injector.getInstance(ConfigImplGenerator.class);
        for (AbstractConfigStructure ast : asts) {
            JavaFile emit = generator.emit(ast);
            try {
                emit.writeTo(processingEnv.getFiler());
            } catch (Exception e) {
                throw new ConfigProcessingException("Could not create config file", e);
            }
        }
        return true;
    }
}
