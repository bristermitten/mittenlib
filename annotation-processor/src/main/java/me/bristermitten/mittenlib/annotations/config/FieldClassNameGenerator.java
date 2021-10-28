package me.bristermitten.mittenlib.annotations.config;

import me.bristermitten.mittenlib.config.names.ConfigName;
import me.bristermitten.mittenlib.config.names.NamingPattern;
import me.bristermitten.mittenlib.config.names.NamingPatternTransformer;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;

public class FieldClassNameGenerator {
    private FieldClassNameGenerator() {
    }

    public static String getConfigFieldName(@NotNull VariableElement element) {
        final ConfigName name = element.getAnnotation(ConfigName.class);
        if (name != null) {
            return name.value();
        }

        NamingPattern annotation = element.getAnnotation(NamingPattern.class);
        Element enclosingElement = element.getEnclosingElement();

        if (annotation == null) {
            while (enclosingElement != null) {
                annotation = enclosingElement.getAnnotation(NamingPattern.class);
                if (annotation != null) {
                    break;
                }
                enclosingElement = enclosingElement.getEnclosingElement();
            }
        }

        if (annotation != null) {
            return NamingPatternTransformer.format(
                    element.getSimpleName().toString(), annotation.value()
            );
        }
        return element.getSimpleName().toString();
    }
}
