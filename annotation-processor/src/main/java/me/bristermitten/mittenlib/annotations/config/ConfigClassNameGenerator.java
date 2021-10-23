package me.bristermitten.mittenlib.annotations.config;

import me.bristermitten.mittenlib.config.Config;

import javax.lang.model.element.TypeElement;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigClassNameGenerator {
    private static final Pattern SUFFIX_PATTERN = Pattern.compile("(.+)(DAO|Config)");

    private ConfigClassNameGenerator() {
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
