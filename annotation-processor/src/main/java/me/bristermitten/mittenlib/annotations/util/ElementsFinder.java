package me.bristermitten.mittenlib.annotations.util;

import javax.inject.Inject;
import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import java.util.List;


/**
 * Helper class for working with {@link Elements}
 */
public class ElementsFinder {

    private final Elements elements;

    @Inject
    ElementsFinder(Elements elements) {
        this.elements = elements;
    }

    /**
     * Get all the {@link VariableElement}s in a given {@link TypeElement}
     * that should be included in the generated config class.
     * This does not include fields with the <code>transient</code> or <code>static</code>
     * modifiers.
     * Fields inherited from superclasses will be included as long as they pass the above checks.
     */
    public List<VariableElement> getApplicableVariableElements(TypeElement rootElement) {
        return elements.getAllMembers(rootElement).stream()
                .filter(element -> element.getKind().isField())
                .map(VariableElement.class::cast)
                .filter(elem -> !elem.getModifiers().contains(Modifier.TRANSIENT)) // ignore transient fields
                .filter(elem -> !elem.getModifiers().contains(Modifier.STATIC)) // ignore static fields
                .toList();
    }

    /**
     * Get all the methods in a given {@link TypeElement}, including inherited ones.
     * This will only return methods, not constructors or initializers.
     */
    public List<ExecutableElement> getAllMethods(TypeElement rootElement) {
        return elements.getAllMembers(rootElement).stream()
                .filter(element -> element.getKind() == ElementKind.METHOD)
                .map(ExecutableElement.class::cast)
                .toList();
    }


}
