package me.bristermitten.mittenlib.annotations.util;

import com.squareup.javapoet.JavaFile;

import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;

/**
 * Generic utilities for converting objects to strings.
 */
public class Stringify {
    private Stringify() {

    }

    /**
     * Returns a string value representing the fully qualified name of the Java class file.
     *
     * @param javaFile the JavaFile object representing the class file
     * @return a string value representing the fully qualified name of the class,
     * consisting of the package name and the class name separated by a dot (.)
     */
    public static String fullyQualifiedName(JavaFile javaFile) {
        return javaFile.packageName + "." + javaFile.typeSpec.name;
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
