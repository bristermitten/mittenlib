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

public class ConfigClassNameGenerator {
    private static final Pattern SUFFIX_PATTERN = Pattern.compile("(.+)(DTO|Config)");

    private final ProcessingEnvironment environment;

    @Inject
    ConfigClassNameGenerator(ProcessingEnvironment environment) {
        this.environment = environment;
    }

    public static Optional<String> generateConfigClassName(TypeElement configDAOType) {
        final Config annotation = configDAOType.getAnnotation(Config.class);
        if (annotation != null && !annotation.className().isEmpty()) {
            return Optional.of(annotation.className());
        }
        final Matcher matcher = SUFFIX_PATTERN.matcher(configDAOType.getSimpleName());
        if (matcher.find()) {
            return Optional.ofNullable(matcher.group(1));
        }
        return Optional.empty();
    }

    /**
     * Generates a ClassName for the actual generated Config class from a given DTO
     * This will include package name.
     * This returns an empty optional if the given type is not a config DTO.
     * Being a config DTO is defined by having a Config annotation and a class name ending in "DTO" or "Config".
     * The generated class name will be the same as the given type, but with the suffix removed.
     * It can also be manually specified in the Config annotation with {@link Config#className()}
     *
     * @param configDTOType The DTO type
     * @return The generated ClassName
     */
    public Optional<ClassName> generateFullConfigClassName(TypeElement configDTOType) {
        final var packageOf = environment.getElementUtils().getPackageOf(configDTOType);
        final String packageName = packageOf.isUnnamed() ? "" : packageOf.toString();
        if (configDTOType.getNestingKind() == NestingKind.MEMBER) {
            // nested class needs to translate
            final var enclosingElement = configDTOType.getEnclosingElement();
            return generateFullConfigClassName((TypeElement) enclosingElement)
                    .flatMap(className -> generateConfigClassName(configDTOType)
                            .map(className::nestedClass));
        }
        return generateConfigClassName(configDTOType)
                .map(simpleClassName -> ClassName.get(packageName, simpleClassName));
    }
}
