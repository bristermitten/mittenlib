package me.bristermitten.mittenlib.annotations.config;

import me.bristermitten.mittenlib.annotations.ast.Property;
import me.bristermitten.mittenlib.annotations.util.TypesUtil;
import me.bristermitten.mittenlib.config.DeserializationContext;
import me.bristermitten.mittenlib.config.names.ConfigName;
import me.bristermitten.mittenlib.config.names.NamingPattern;
import me.bristermitten.mittenlib.config.names.NamingPatternTransformer;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.lang.model.element.VariableElement;

/**
 * Responsible for generating serial keys based on DTO fields
 */
public class FieldClassNameGenerator {
    private final TypesUtil typesUtil;

    @Inject
    FieldClassNameGenerator(TypesUtil typesUtil) {
        this.typesUtil = typesUtil;
    }

    /**
     * Get a suitable serialization key for a given DTO field.
     * This is the String that is looked up from the given {@link DeserializationContext#getData()}
     * <p>
     * This method takes into account a number of things:
     * 1. A {@link ConfigName} annotation, if present.
     * 2. A {@link NamingPattern} annotation, if present.
     * 3. The name of the field itself
     * <p>
     * The first match from this list is returned as the key.
     *
     * @param element The DTO field
     * @return The key to use when reading from {@link DeserializationContext#getData()} for the given field.
     */
    public String getConfigFieldName(@NotNull VariableElement element) {
        final ConfigName name = element.getAnnotation(ConfigName.class);
        if (name != null) {
            return name.value();
        }

        NamingPattern annotation = typesUtil.getAnnotation(element, NamingPattern.class);

        if (annotation != null) {
            return NamingPatternTransformer.format(
                    element.getSimpleName().toString(), annotation.value()
            );
        }
        return element.getSimpleName().toString();
    }

    public String getConfigFieldName(@NotNull Property property) {
        ConfigName configName = property.settings().configName();

        if (configName != null) {
            return configName.value();
        }

        NamingPattern namingPattern = property.settings().namingPattern();


        if (namingPattern != null) {
            return NamingPatternTransformer.format(
                    property.name(), namingPattern.value()
            );
        }
        return property.name();
    }
}
