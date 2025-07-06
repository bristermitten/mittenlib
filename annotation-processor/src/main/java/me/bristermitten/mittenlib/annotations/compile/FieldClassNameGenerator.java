package me.bristermitten.mittenlib.annotations.compile;

import me.bristermitten.mittenlib.annotations.ast.Property;
import me.bristermitten.mittenlib.config.DeserializationContext;
import me.bristermitten.mittenlib.config.names.ConfigName;
import me.bristermitten.mittenlib.config.names.NamingPattern;
import me.bristermitten.mittenlib.config.names.NamingPatternTransformer;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

/**
 * Responsible for generating serial keys based on DTO fields
 */
public class FieldClassNameGenerator {

    @Inject
    FieldClassNameGenerator() {
    }

    /**
     * Get a suitable serialization key for a given property.
     * This is the String that is looked up from the given {@link DeserializationContext#getData()}
     *
     * @param property The property
     * @return The key to use when reading from {@link DeserializationContext#getData()} for the given property.
     */
    public String getConfigFieldName(@NotNull Property property) {
        ConfigName configName = property.settings().configName();
        NamingPattern namingPattern = property.settings().namingPattern();
        String fieldName = property.name();

        return getConfigFieldName(configName, namingPattern, fieldName);
    }

    /**
     * Helper method to get the config field name based on annotations and field name.
     *
     * @param configName The ConfigName annotation, if present
     * @param namingPattern The NamingPattern annotation, if present
     * @param fieldName The name of the field
     * @return The config field name
     */
    private String getConfigFieldName(ConfigName configName, NamingPattern namingPattern, String fieldName) {
        if (configName != null) {
            return configName.value();
        }

        if (namingPattern != null) {
            return NamingPatternTransformer.format(fieldName, namingPattern.value());
        }

        return fieldName;
    }
}
