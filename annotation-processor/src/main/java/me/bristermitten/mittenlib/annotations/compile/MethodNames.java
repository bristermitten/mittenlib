package me.bristermitten.mittenlib.annotations.compile;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.bristermitten.mittenlib.annotations.ast.Property;
import me.bristermitten.mittenlib.annotations.util.ElementsFinder;
import me.bristermitten.mittenlib.util.Strings;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Manages method names for configuration classes to ensure they don't conflict with existing
 * methods. This class caches method names to improve performance and provides utilities to generate
 * safe method names that won't cause conflicts in the generated code.
 */
@Singleton
public class MethodNames {
    private final ElementsFinder elementsFinder;
    private final Map<VariableElement, String> safeNameCache = new HashMap<>();
    private final Map<VariableElement, Set<String>> methodNamesCache = new HashMap<>();

    /**
     * The prefix for all generated serialization methods. For example, a method to serialize a field
     * called "test" would be called {@code serializeTest}
     */
    private static final String SERIALIZE_METHOD_PREFIX = "serialize";

    /**
     * The prefix for all generated deserialization methods. For example, a method to deserialize a
     * field called "test" would be called {@code deserializeTest}
     */
    private static final String DESERIALIZE_METHOD_PREFIX = "deserialize";

    @Inject
    MethodNames(ElementsFinder elementsFinder) {
        this.elementsFinder = elementsFinder;
    }

    /**
     * Gets a safe method name for a variable element that doesn't conflict with existing methods.
     * This method uses caching to improve performance for repeated calls with the same element.
     *
     * @param variableElement The variable element to generate a method name for
     * @param enclosingClass The class that encloses the variable element
     * @return A safe method name that doesn't conflict with existing methods
     */
    public String safeMethodName(VariableElement variableElement, TypeElement enclosingClass) {
        return safeNameCache.computeIfAbsent(variableElement, elem -> safeMethodName0(elem, enclosingClass));
    }

    /**
     * Gets a safe method name for a property that doesn't conflict with existing methods. This method
     * handles both field-based and method-based properties differently: - For field-based properties,
     * it delegates to {@link #safeMethodName(VariableElement, TypeElement)} - For method-based
     * properties, it uses the method's simple name directly
     *
     * @param property The property to generate a method name for
     * @return A safe method name that doesn't conflict with existing methods
     */
    public String safeMethodName(Property property) {
        return switch (property.source()) {
            case Property.PropertySource.FieldSource(var field) ->
                safeMethodName(field, (TypeElement) field.getEnclosingElement());
            case Property.PropertySource.MethodSource(var method) ->
                method.getSimpleName().toString();
        };
    }

    private String safeMethodName0(VariableElement variableElement, TypeElement enclosingClass) {
        var methodNames = methodNamesCache.computeIfAbsent(variableElement, x -> getNoArgMethodNames(enclosingClass));

        var name = new StringBuilder(variableElement.getSimpleName());
        while (methodNames.contains(name.toString())) {
            name.append("_");
        }
        return name.toString();
    }

    private Set<String> getNoArgMethodNames(TypeElement enclosingClass) {
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
     * Gets the name of the deserialization method for a property. This should only be used when the
     * type is known to be a configuration implementation class.
     *
     * @param property the property
     * @return The deserialization method name
     */
    public String getDeserializeMethodName(Property property) {
        var name = Strings.capitalize(property.name());
        return DESERIALIZE_METHOD_PREFIX + name;
    }

    /**
     * Gets the name of the serialization method for a property. This should only be used when the
     * type is known to be a configuration implementation class.
     *
     * @param property the property
     * @return The serialization method name
     */
    public String getSerializeMethodName(Property property) {
        var name = Strings.capitalize(property.name());
        return SERIALIZE_METHOD_PREFIX + name;
    }
}
