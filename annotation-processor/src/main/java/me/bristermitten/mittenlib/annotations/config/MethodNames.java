package me.bristermitten.mittenlib.annotations.config;

import me.bristermitten.mittenlib.annotations.util.ElementsFinder;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.stream.Collectors;

public class MethodNames {
    private final ElementsFinder elementsFinder;

    public MethodNames(ElementsFinder elementsFinder) {
        this.elementsFinder = elementsFinder;
    }

    public String safeMethodName(VariableElement variableElement, TypeElement enclosingClass) {
        var name = new StringBuilder(variableElement.getSimpleName());
        var methodNames = elementsFinder.getAllMethods(enclosingClass)
                .stream()
                .filter(elem -> elem.getParameters().isEmpty()) // no args
                .map(elem -> elem.getSimpleName().toString())
                .collect(Collectors.toSet());

        while (methodNames.contains(name.toString())) {
            name.append("_");
        }
        return name.toString();
    }
}
