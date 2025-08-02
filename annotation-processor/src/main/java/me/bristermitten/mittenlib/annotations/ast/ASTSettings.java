package me.bristermitten.mittenlib.annotations.ast;

import me.bristermitten.mittenlib.config.Config;
import me.bristermitten.mittenlib.config.EnumParsingSchemes;
import me.bristermitten.mittenlib.config.Source;
import me.bristermitten.mittenlib.config.names.ConfigName;
import me.bristermitten.mittenlib.config.names.NamingPattern;
import org.jspecify.annotations.Nullable;

public sealed interface ASTSettings {
    @Nullable
    NamingPattern namingPattern();

    record ConfigASTSettings(
            @Nullable NamingPattern namingPattern,
            @Nullable Source source,
            Config config,
            boolean generateToString
    ) implements ASTSettings {
    }

    record PropertyASTSettings(
            @Nullable NamingPattern namingPattern,
            @Nullable ConfigName configName,
            EnumParsingSchemes enumParsingScheme,
            boolean isNullable,
            boolean hasDefaultValue
    ) implements ASTSettings {
    }
}
