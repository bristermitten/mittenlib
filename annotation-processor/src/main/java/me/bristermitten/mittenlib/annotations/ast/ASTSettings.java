package me.bristermitten.mittenlib.annotations.ast;

import me.bristermitten.mittenlib.config.Config;
import me.bristermitten.mittenlib.config.EnumParsingSchemes;
import me.bristermitten.mittenlib.config.Source;
import me.bristermitten.mittenlib.config.names.ConfigName;
import me.bristermitten.mittenlib.config.names.NamingPattern;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Abstract view of configuration options for some {@link AbstractConfigStructure}.
 */
public sealed interface ASTSettings {
    /**
     * Returns the {@link NamingPattern}, if set.
     */
    @Nullable NamingPattern namingPattern();

    /**
     * Configuration options for a config type.
     *
     * @param namingPattern    The global config-level {@link NamingPattern}, if present.
     * @param source           The {@link Source} annotation present on the config type, if present.
     * @param config           the original {@link Config} annotation present on the config type
     * @param generateToString Whether we should generate a {@code toString()} method for the config
     *                         implementation
     */
    record ConfigASTSettings(
            @Nullable NamingPattern namingPattern,
            @Nullable Source source,
            Config config,
            boolean generateToString)
            implements ASTSettings {
    }

    /**
     * Configuration options for a config property
     *
     * @param namingPattern     The property-level {@link NamingPattern}, if present
     * @param configName        The property-level {@link ConfigName}, if present
     * @param enumParsingScheme The {@link EnumParsingSchemes} that should be used for this property,
     *                          if it is an enum.
     * @param isNullable        whether the property is declared as nullable
     * @param hasDefaultValue   whether the property has a default value
     * @param constraints       any validation constraints that should be applied to this property
     */
    record PropertyASTSettings(
            @Nullable NamingPattern namingPattern,
            @Nullable ConfigName configName,
            EnumParsingSchemes enumParsingScheme,
            boolean isNullable,
            boolean hasDefaultValue,
            List<ValidationConstraint> constraints)
      implements ASTSettings {}
}
