package me.bristermitten.mittenlib.annotations.config;

import com.squareup.javapoet.ClassName;
import me.bristermitten.mittenlib.config.Config;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigClassNameGenerator {
    private static final Pattern SUFFIX_PATTERN = Pattern.compile("(.+)(DTO|Config)");

    private ConfigClassNameGenerator() {
    }

    public static Optional<ClassName> generateFullConfigClassName(ProcessingEnvironment environment, TypeElement configDAOType) {
        final var packageOf = environment.getElementUtils().getPackageOf(configDAOType);
        final String packageName = packageOf.isUnnamed() ? "" : packageOf.toString();
        if (configDAOType.getNestingKind() == NestingKind.MEMBER) {
            // nested class needs to translate
            final var enclosingElement = configDAOType.getEnclosingElement();
            return generateFullConfigClassName(environment, (TypeElement) enclosingElement)
                    .flatMap(className -> generateConfigClassName(configDAOType)
                             .map(className::nestedClass));
        }
        return generateConfigClassName(configDAOType)
                .map(simpleClassName -> ClassName.get(packageName, simpleClassName));
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
}
