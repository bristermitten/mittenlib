package me.bristermitten.mittenlib.annotations.util;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.List;

public class ElementsFinder {
    private final Types types;

    public ElementsFinder(Types types) {
        this.types = types;
    }

    public List<VariableElement> getApplicableVariableElements(Element rootElement) {
        return getAllEnclosedElements(rootElement).stream()
                .filter(element -> element.getKind().isField())
                .map(VariableElement.class::cast)
                .filter(elem -> !elem.getModifiers().contains(Modifier.TRANSIENT)) // ignore transient fields
                .filter(elem -> !elem.getModifiers().contains(Modifier.STATIC)) // ignore static fields
                .toList();
    }

    private List<Element> getAllEnclosedElements(Element rootElement) {
        var inElement = new ArrayList<Element>(rootElement.getEnclosedElements());
        if (rootElement instanceof TypeElement type) {
            TypeMirror superclass = type.getSuperclass();
            if (superclass.getKind() != TypeKind.NONE) {
                Element superElement = types.asElement(superclass);
                if (superElement != null) {
                    List<Element> allEnclosedElements = getAllEnclosedElements(superElement);
                    inElement.addAll(allEnclosedElements);
                }
            }
        }
        return inElement;
    }


}
