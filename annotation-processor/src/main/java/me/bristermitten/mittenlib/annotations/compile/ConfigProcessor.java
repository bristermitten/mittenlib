package me.bristermitten.mittenlib.annotations.compile;

import com.google.auto.service.AutoService;
import com.google.inject.Guice;
import com.squareup.javapoet.JavaFile;
import io.toolisticon.aptk.common.ToolingProvider;
import io.toolisticon.aptk.tools.AbstractAnnotationProcessor;
import me.bristermitten.mittenlib.annotations.ast.AbstractConfigStructure;
import me.bristermitten.mittenlib.annotations.exception.ConfigProcessingException;
import me.bristermitten.mittenlib.annotations.parser.ConfigClassParser;
import me.bristermitten.mittenlib.config.Config;
import org.jetbrains.annotations.NotNull;

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
 * Annotation processor for generating configuration classes from DTO classes marked with {@link Config}.
 * This processor handles the compilation-time generation of implementation classes for configuration DTOs,
 * creating strongly-typed configuration objects with proper getters, equals, hashCode, and toString methods.
 * 
 * The processor only processes top-level classes (not nested classes) and uses Guice for dependency injection
 * of its internal components.
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

    /**
     * Processes annotations and generates configuration implementation classes.
     * This method is called by the Java compiler during the annotation processing phase.
     * It performs the following steps:
     * 1. Sets up the tooling environment and creates a Guice injector
     * 2. Finds all top-level classes annotated with @Config
     * 3. Parses each class into an abstract configuration structure
     * 4. Generates implementation classes for each structure
     * 5. Writes the generated files to the filer
     *
     * @param annotations The annotation types requested to be processed
     * @param roundEnv The environment for this round of annotation processing
     * @return true if the annotations were processed successfully, false otherwise
     * @throws ConfigProcessingException if there is an error writing the generated files
     */
    @Override
    public boolean processAnnotations(@NotNull Set<? extends TypeElement> annotations, @NotNull RoundEnvironment roundEnv) {

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
