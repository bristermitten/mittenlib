package me.bristermitten.mittenlib.annotations.util;

import com.squareup.javapoet.JavaFile;

import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;

public class Stringify {
    private Stringify() {
    
    }

    public static String stringify(JavaFile javaFile) {
        return javaFile.packageName + "." + javaFile.typeSpec.name;
    }

    public static String stringify(Element element) {
        if (element instanceof VariableElement variableElement) {
            return variableElement.asType() + " " + variableElement.getSimpleName() + " in class " + stringify(variableElement.getEnclosingElement());
        }

        return element.toString();
    }
}
