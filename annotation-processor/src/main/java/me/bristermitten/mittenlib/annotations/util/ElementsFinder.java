package me.bristermitten.mittenlib.annotations.util;

import javax.inject.Inject;
import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import java.util.List;

public class ElementsFinder {

    private final Elements elements;

    @Inject
    ElementsFinder(Elements elements) {
        this.elements = elements;
    }

    public List<VariableElement> getApplicableVariableElements(TypeElement rootElement) {
        return getAllEnclosedElements(rootElement).stream()
                .filter(element -> element.getKind().isField())
                .map(VariableElement.class::cast)
                .filter(elem -> !elem.getModifiers().contains(Modifier.TRANSIENT)) // ignore transient fields
                .filter(elem -> !elem.getModifiers().contains(Modifier.STATIC)) // ignore static fields
                .toList();
    }

    public List<ExecutableElement> getAllMethods(TypeElement rootElement) {
        return getAllEnclosedElements(rootElement).stream()
                .filter(element -> element.getKind() == ElementKind.METHOD)
                .map(ExecutableElement.class::cast)
                .toList();
    }

    private List<? extends Element> getAllEnclosedElements(TypeElement rootElement) {
        return elements.getAllMembers(rootElement);
    }


}
