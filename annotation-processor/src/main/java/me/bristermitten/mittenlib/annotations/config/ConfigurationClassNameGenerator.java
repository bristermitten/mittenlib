package me.bristermitten.mittenlib.annotations.config;

import com.squareup.javapoet.ClassName;
import me.bristermitten.mittenlib.config.Config;

import javax.annotation.processing.ProcessingEnvironment;
import javax.inject.Inject;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Responsible for generating proper class names for config classes
 */
public class ConfigurationClassNameGenerator {
    private static final Pattern SUFFIX_PATTERN = Pattern.compile("(.+)(DTO|Config)");

    private final ProcessingEnvironment environment;

    @Inject
    ConfigurationClassNameGenerator(ProcessingEnvironment environment) {
        this.environment = environment;
    }

    /**
     * Translates a {@link TypeElement} into its non-DTO name by
     * reading a {@link Config#className()} or removing the suffix.
     *
     * @param dtoType the DTO type
     * @return the non-DTO name, if possible. An empty optional implies that the type is not a valid DTO.
     */
    private static Optional<String> findConfigClassName(TypeElement dtoType) {
        final Config annotation = dtoType.getAnnotation(Config.class);
        if (annotation != null && !annotation.className().isEmpty()) {
            return Optional.of(annotation.className());
        }
        final Matcher matcher = SUFFIX_PATTERN.matcher(dtoType.getSimpleName());
        if (matcher.find()) {
            return Optional.ofNullable(matcher.group(1));
        }
        return Optional.empty();
    }

    /**
     * Generates a ClassName for the actual generated configuration class from a given DTO, using the package
     * of the given {@link TypeElement}.
     * <p>
     * This returns an empty optional if the given type is not a DTO type, which is defined by the following rules:
     * <ol>
     * <li>The type must be annotated with {@link Config}</li>
     * <li>The type must be a class</li>
     * <li>The class name must end with "DTO" or "Config"</li>
     * </ol>
     * <p>
     * The returned class name will be the same as the given type, but with the suffix removed.
     * It can also be manually specified in the Config annotation with {@link Config#className()}
     *
     * @param configDTOType The DTO type
     * @return The generated ClassName
     */
    public Optional<ClassName> generateConfigurationClassName(TypeElement configDTOType) {
        if (configDTOType.getNestingKind() == NestingKind.MEMBER) {
            /*
            If the type is a nested class, then we first translate the enclosing class name (which may do nothing),
            then create a nested class name.
             */

            final var enclosingElement = configDTOType.getEnclosingElement();
            return generateConfigurationClassName((TypeElement) enclosingElement)
                    .flatMap(className ->
                            findConfigClassName(configDTOType)
                                    .map(className::nestedClass));

        }

        final String packageName = environment.getElementUtils().getPackageOf(configDTOType).getQualifiedName().toString();
        return findConfigClassName(configDTOType)
                .map(simpleClassName -> ClassName.get(packageName, simpleClassName));
    }
}
