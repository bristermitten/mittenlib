package me.bristermitten.mittenlib.annotations.config;

import com.google.inject.Singleton;
import me.bristermitten.mittenlib.annotations.ast.Property;
import me.bristermitten.mittenlib.annotations.util.ElementsFinder;

import javax.inject.Inject;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Singleton
public class MethodNames {
    private final ElementsFinder elementsFinder;
    private final Map<VariableElement, String> safeNameCache = new HashMap<>();
    private final Map<VariableElement, Set<String>> safeNameCache2 = new HashMap<>();

    @Inject
    MethodNames(ElementsFinder elementsFinder) {
        this.elementsFinder = elementsFinder;
    }

    public String safeMethodName(VariableElement variableElement, TypeElement enclosingClass) {
        return safeNameCache.computeIfAbsent(variableElement,
                elem -> safeMethodName0(elem, enclosingClass));
    }

    public String safeMethodName(Property property) {
        return switch (property.source()) {
            case Property.PropertySource.FieldSource(var field) ->
                    safeMethodName(field, (TypeElement) field.getEnclosingElement());
            case Property.PropertySource.MethodSource(var method) -> method.getSimpleName().toString();
        };
    }

    private String safeMethodName0(VariableElement variableElement, TypeElement enclosingClass) {
        var methodNames = safeNameCache2.computeIfAbsent(variableElement, x -> getNoArgMethodNames(enclosingClass));

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
}
