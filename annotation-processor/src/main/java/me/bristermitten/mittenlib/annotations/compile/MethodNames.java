package me.bristermitten.mittenlib.annotations.compile;

import com.google.inject.Singleton;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import me.bristermitten.mittenlib.annotations.ast.AbstractConfigStructure;
import me.bristermitten.mittenlib.annotations.ast.Property;
import me.bristermitten.mittenlib.annotations.util.ElementsFinder;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Manages method names for configuration classes to ensure they don't conflict with existing methods.
 * This class caches method names to improve performance and provides utilities to generate
 * safe method names that won't cause conflicts in the generated code.
 */
@Singleton
public class MethodNames {
    private final ElementsFinder elementsFinder;
    private final Map<VariableElement, String> safeNameCache = new HashMap<>();
    private final Map<VariableElement, Set<String>> methodNamesCache = new HashMap<>();
    private final ConfigurationClassNameGenerator configurationClassNameGenerator;

    @Inject
    MethodNames(ElementsFinder elementsFinder, ConfigurationClassNameGenerator configurationClassNameGenerator) {
        this.elementsFinder = elementsFinder;
        this.configurationClassNameGenerator = configurationClassNameGenerator;
    }

    /**
     * Gets a safe method name for a variable element that doesn't conflict with existing methods.
     * This method uses caching to improve performance for repeated calls with the same element.
     *
     * @param variableElement The variable element to generate a method name for
     * @param enclosingClass  The class that encloses the variable element
     * @return A safe method name that doesn't conflict with existing methods
     */
    public @NotNull String safeMethodName(VariableElement variableElement, TypeElement enclosingClass) {
        return safeNameCache.computeIfAbsent(variableElement,
                elem -> safeMethodName0(elem, enclosingClass));
    }

    /**
     * Gets a safe method name for a property that doesn't conflict with existing methods.
     * This method handles both field-based and method-based properties differently:
     * - For field-based properties, it delegates to {@link #safeMethodName(VariableElement, TypeElement)}
     * - For method-based properties, it uses the method's simple name directly
     *
     * @param property The property to generate a method name for
     * @return A safe method name that doesn't conflict with existing methods
     */
    public String safeMethodName(@NotNull Property property) {
        return switch (property.source()) {
            case Property.PropertySource.FieldSource(var field) ->
                    safeMethodName(field, (TypeElement) field.getEnclosingElement());
            case Property.PropertySource.MethodSource(var method) -> method.getSimpleName().toString();
        };
    }

    private @NotNull String safeMethodName0(@NotNull VariableElement variableElement, TypeElement enclosingClass) {
        var methodNames = methodNamesCache.computeIfAbsent(variableElement, x -> getNoArgMethodNames(enclosingClass));

        var name = new StringBuilder(variableElement.getSimpleName());
        while (methodNames.contains(name.toString())) {
            name.append("_");
        }
        return name.toString();
    }

    private @NotNull Set<String> getNoArgMethodNames(TypeElement enclosingClass) {
        var names = new HashSet<String>();
        for (ExecutableElement method : elementsFinder.getAllMethods(enclosingClass)) {
            if (!method.getParameters().isEmpty()) {
                continue;
            }
            names.add(method.getSimpleName().toString());
        }
        return names;
    }

    /**
     * Gets the name of the deserialization method for a type.
     *
     * @param name The type name
     * @return The deserialization method name
     */
    @Deprecated
    public @NotNull String getDeserializeMethodName(TypeName name) {
        if (name instanceof ClassName cn) {
            return DeserializationCodeGenerator.DESERIALIZE_METHOD_PREFIX + cn.simpleName();
        }
        return DeserializationCodeGenerator.DESERIALIZE_METHOD_PREFIX + name;
    }

    /**
     * Gets the name of the deserialization method for a configuration structure.
     * This method uses the implementation class name derived from the structure.
     *
     * @param ast The abstract configuration structure
     * @return The deserialization method name for the structure
     */
    public String getDeserializeMethodName(@NotNull AbstractConfigStructure ast) {
        return getDeserializeMethodName(configurationClassNameGenerator.translateConfigClassName(ast));
    }
}
