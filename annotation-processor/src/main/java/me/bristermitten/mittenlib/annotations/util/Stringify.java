package me.bristermitten.mittenlib.annotations.util;

import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;

/**
 * Generic utilities for converting objects to strings.
 */
public class Stringify {
    private Stringify() {

    }

    /**
     * Returns a generic pretty string representation of the given {@link Element} object.
     * <p>
     * For a VariableElement, the format is "{type} {name} in class {enclosingClass}",
     *
     * @param element the Element object to generate the string representation of
     * @return a string representation of the given Element object
     */
    public static String prettyStringify(Element element) {
        if (element instanceof VariableElement variableElement) {
            return variableElement.asType() + " " + variableElement.getSimpleName() + " in class " + prettyStringify(variableElement.getEnclosingElement());
        }

        return element.toString();
    }
}
