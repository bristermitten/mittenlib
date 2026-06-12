package me.bristermitten.mittenlib.annotations.config;

import com.google.auto.service.AutoService;
import com.google.inject.Guice;
import com.palantir.javapoet.JavaFile;
import io.toolisticon.aptk.common.ToolingProvider;
import io.toolisticon.aptk.tools.AbstractAnnotationProcessor;
import io.toolisticon.aptk.tools.MessagerUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import me.bristermitten.mittenlib.annotations.ast.AbstractConfigStructure;
import me.bristermitten.mittenlib.annotations.compile.ConfigImplGenerator;
import me.bristermitten.mittenlib.annotations.compile.ConfigLoaderGenerator;
import me.bristermitten.mittenlib.annotations.compile.ConfigLoaderModuleGenerator;
import me.bristermitten.mittenlib.annotations.compile.ConfigProcessorModule;
import me.bristermitten.mittenlib.annotations.compile.ConfigSaverGenerator;
import me.bristermitten.mittenlib.annotations.compile.ConfigValidatorGenerator;
import me.bristermitten.mittenlib.annotations.compile.ConfigurationClassNameGenerator;
import me.bristermitten.mittenlib.annotations.compile.NewtypeImplGenerator;
import me.bristermitten.mittenlib.annotations.exception.ConfigProcessingException;
import me.bristermitten.mittenlib.annotations.parser.ASTVerifier;
import me.bristermitten.mittenlib.annotations.parser.ConfigClassParser;
import me.bristermitten.mittenlib.annotations.parser.CustomDeserializers;
import me.bristermitten.mittenlib.annotations.parser.CustomSerializers;
import me.bristermitten.mittenlib.config.Config;
import me.bristermitten.mittenlib.config.Newtype;
import me.bristermitten.mittenlib.config.extension.CustomDeserializerFor;
import me.bristermitten.mittenlib.config.extension.CustomSerializerFor;

/**
 * Annotation processor for generating configuration classes from DTO classes marked with {@link
 * Config}. This processor handles the compilation-time generation of implementation classes for
 * configuration DTOs, creating strongly typed configuration objects with proper getters, equals,
 * hashCode, and toString methods. The processor only processes top-level classes (not nested
 * classes).
 */
@SupportedAnnotationTypes({"me.bristermitten.mittenlib.config.Config", "me.bristermitten.mittenlib.config.Newtype"})
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@AutoService(Processor.class)
public class ConfigProcessor extends AbstractAnnotationProcessor {

    /** Public constructor for the compiler */
    public ConfigProcessor() {
        super();
    }

    /**
     * Processes annotations and generates configuration implementation classes. This method is called
     * by the Java compiler during the annotation processing phase. It performs the following steps:
     * 1. Sets up the tooling environment and creates a Guice injector 2. Finds all top-level classes
     * annotated with @Config 3. Parses each class into an abstract configuration structure 4.
     * Generates implementation classes for each structure 5. Writes the generated files to the filer
     *
     * @param annotations The annotation types requested to be processed
     * @param roundEnv The environment for this round of annotation processing
     * @return true if the annotations were processed successfully, false otherwise
     * @throws ConfigProcessingException if there is an error writing the generated files
     */
    @Override
    public boolean processAnnotations(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        ToolingProvider.setTooling(processingEnv);
        var injector = Guice.createInjector(new ConfigProcessorModule(processingEnv));

        final List<TypeElement> types = roundEnv.getElementsAnnotatedWith(Config.class).stream()
                .filter(TypeElement.class::isInstance)
                .map(TypeElement.class::cast)
                .filter(element -> element.getNestingKind() == NestingKind.TOP_LEVEL)
                .toList();

        final List<TypeElement> newtypes = roundEnv.getElementsAnnotatedWith(Newtype.class).stream()
                .filter(TypeElement.class::isInstance)
                .map(TypeElement.class::cast)
                .toList();

        boolean anyErrors = false;

        var newtypeGenerator = new NewtypeImplGenerator();
        for (TypeElement newtype : newtypes) {
            if (newtype.getKind() != ElementKind.RECORD && newtype.getKind() != ElementKind.INTERFACE) {
                MessagerUtils.error(
                        newtype,
                        "Newtype annotation only supports records and interfaces: " + newtype.getQualifiedName());
                anyErrors = true;
                continue;
            }

            if (newtype.getKind() == ElementKind.RECORD) {
                if (newtype.getRecordComponents().size() != 1) {
                    MessagerUtils.error(
                            newtype,
                            "Newtype record " + newtype.getQualifiedName() + " must have exactly one component");
                    anyErrors = true;
                }
            } else if (newtype.getKind() == ElementKind.INTERFACE) {
                List<ExecutableElement> methods = ElementFilter.methodsIn(newtype.getEnclosedElements()).stream()
                        .filter(m -> !m.isDefault() && !m.getModifiers().contains(Modifier.STATIC))
                        .toList();
                if (methods.size() != 1) {
                    MessagerUtils.error(
                            newtype,
                            "Newtype interface " + newtype.getQualifiedName()
                                    + " must have exactly one abstract method");
                    anyErrors = true;
                    continue;
                }

                if (!anyErrors) {
                    JavaFile emit = newtypeGenerator.emit(newtype);
                    try {
                        emit.writeTo(processingEnv.getFiler());
                    } catch (Exception e) {
                        throw new ConfigProcessingException("Could not create newtype impl file", e);
                    }
                }
            }
        }

        CustomDeserializers customDeserializers = injector.getInstance(CustomDeserializers.class);
        roundEnv.getElementsAnnotatedWith(CustomDeserializerFor.class).stream()
                .map(TypeElement.class::cast)
                .forEach(customDeserializers::registerCustomDeserializer);

        CustomSerializers customSerializers = injector.getInstance(CustomSerializers.class);
        roundEnv.getElementsAnnotatedWith(CustomSerializerFor.class).stream()
                .map(TypeElement.class::cast)
                .forEach(customSerializers::registerCustomSerializer);

        List<AbstractConfigStructure> asts = new ArrayList<>();
        var configClassParser = injector.getInstance(ConfigClassParser.class);
        for (TypeElement clazz : types) {
            var ast = configClassParser.parseAbstract(clazz);
            asts.add(ast);
        }

        ASTVerifier verifier = injector.getInstance(ASTVerifier.class);
        for (AbstractConfigStructure ast : asts) {
            if (!verifier.verify(ast)) {
                anyErrors = true;
            }
        }

        if (anyErrors) {
            return true;
        }

        var generator = injector.getInstance(ConfigImplGenerator.class);
        var loaderGenerator = injector.getInstance(ConfigLoaderGenerator.class);
        var saverGenerator = injector.getInstance(ConfigSaverGenerator.class);
        var validatorGenerator = injector.getInstance(ConfigValidatorGenerator.class);
        for (AbstractConfigStructure ast : asts) {
            JavaFile emit = generator.emit(ast);
            JavaFile loaderEmit = loaderGenerator.emit(ast);
            JavaFile saverEmit = saverGenerator.emit(ast);
            JavaFile validatorEmit = validatorGenerator.emit(ast);
            try {
                emit.writeTo(processingEnv.getFiler());
                loaderEmit.writeTo(processingEnv.getFiler());
                saverEmit.writeTo(processingEnv.getFiler());
                validatorEmit.writeTo(processingEnv.getFiler());
            } catch (Exception e) {
                throw new ConfigProcessingException("Could not create config file", e);
            }
        }

        if (!asts.isEmpty()) {
            var moduleGenerator = injector.getInstance(ConfigLoaderModuleGenerator.class);
            var classNameGenerator = injector.getInstance(ConfigurationClassNameGenerator.class);

            // Sort by package name and then simple name for stability
            asts.sort(Comparator.comparing((AbstractConfigStructure ast) ->
                            classNameGenerator.getPublicClassName(ast).packageName())
                    .thenComparing(
                            ast -> classNameGenerator.getPublicClassName(ast).simpleName()));

            // Use the shortest package name as the "root" package for the module
            String rootPackage = asts.stream()
                    .map(ast -> classNameGenerator.getPublicClassName(ast).packageName())
                    .min(Comparator.comparingInt(String::length))
                    .orElse("");

            JavaFile moduleEmit = moduleGenerator.emit(asts, rootPackage);
            try {
                moduleEmit.writeTo(processingEnv.getFiler());
            } catch (Exception e) {
                throw new ConfigProcessingException("Could not create ConfigLoaderModule file", e);
            }
        }
        return true;
    }
}
