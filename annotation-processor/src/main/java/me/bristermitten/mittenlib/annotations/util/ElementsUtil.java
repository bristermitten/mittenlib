package me.bristermitten.mittenlib.annotations.util;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import java.util.List;

public class ElementsUtil {
    public static List<VariableElement> getApplicableVariableElements(Element rootElement) {
        return rootElement.getEnclosedElements().stream()
                .filter(element -> element.getKind().isField())
                .map(VariableElement.class::cast)
                .filter(elem -> !elem.getModifiers().contains(Modifier.TRANSIENT)) // ignore transient fields
                .filter(elem -> !elem.getModifiers().contains(Modifier.STATIC)) // ignore static fields
                .toList();
    }


}
