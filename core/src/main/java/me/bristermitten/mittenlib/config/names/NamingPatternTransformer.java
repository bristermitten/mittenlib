package me.bristermitten.mittenlib.config.names;

import me.bristermitten.mittenlib.util.Strings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public class NamingPatternTransformer {
    // https://stackoverflow.com/questions/1097901/regular-expression-split-string-by-capital-letter-but-ignore-tla
    private static final Pattern CAMEL_CASE_PATTERN = Pattern.compile("(?=(?<=[a-z])[A-Z]|[A-Z](?=[a-z]))");

    private NamingPatternTransformer() {
    }

    public static String format(@NotNull String input, @NotNull NamingPatterns pattern) {
        if (pattern == NamingPatterns.DEFAULT) {
            return input;
        }
        final String[] parts = CAMEL_CASE_PATTERN.split(input);
        if (parts.length == 0) {
            return input;
        }

        String camelCaseFormat = camelCaseFormat(pattern, parts);
        if (camelCaseFormat != null) return camelCaseFormat;
        final String separator;
        final UnaryOperator<String> transformer;
        if (pattern == NamingPatterns.LOWER_KEBAB_CASE || pattern == NamingPatterns.UPPER_KEBAB_CASE) {
            separator = "-";
            transformer = pattern == NamingPatterns.LOWER_KEBAB_CASE ? String::toLowerCase : Strings::capitalize;
        } else if (pattern == NamingPatterns.LOWER_SNAKE_CASE || pattern == NamingPatterns.UPPER_SNAKE_CASE) {
            separator = "_";
            transformer = pattern == NamingPatterns.LOWER_SNAKE_CASE ? String::toLowerCase : Strings::capitalize;
        } else {
            throw new IllegalStateException("what");
        }


        return Strings.joinWith(
                Arrays.asList(parts),
                transformer::apply,
                separator);
    }

    @Nullable
    private static String camelCaseFormat(@NotNull NamingPatterns pattern, String[] parts) {
        if (pattern != NamingPatterns.LOWER_CAMEL_CASE && pattern != NamingPatterns.UPPER_CAMEL_CASE) {
            return null;
        }
        for (int i = 0; i < parts.length; i++) {
            parts[i] = Strings.capitalize(parts[i]);
        }
        if (pattern == NamingPatterns.LOWER_CAMEL_CASE) {
            parts[0] = Strings.uncapitalize(parts[0]);
        }
        return String.join("", parts);
    }
}
