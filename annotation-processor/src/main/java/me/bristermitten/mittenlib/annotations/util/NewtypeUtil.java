package me.bristermitten.mittenlib.annotations.util;

import com.palantir.javapoet.ClassName;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import me.bristermitten.mittenlib.config.Newtype;

public final class NewtypeUtil {

    private NewtypeUtil() {}

    public static boolean isNewtype(Element element) {
        return element instanceof TypeElement && element.getAnnotation(Newtype.class) != null;
    }

    public static TypeMirror getUnderlyingType(TypeElement element) {
        if (element.getKind() == ElementKind.RECORD) {
            List<? extends RecordComponentElement> components = element.getRecordComponents();
            if (components.size() != 1) {
                throw new IllegalArgumentException("Newtype record " + element + " must have exactly one component");
            }
            return components.getFirst().asType();
        } else if (element.getKind() == ElementKind.INTERFACE) {
            List<ExecutableElement> methods = ElementFilter.methodsIn(element.getEnclosedElements()).stream()
                    .filter(m -> !m.isDefault() && !m.getModifiers().contains(Modifier.STATIC))
                    .toList();
            if (methods.size() != 1) {
                throw new IllegalArgumentException(
                        "Newtype interface " + element + " must have exactly one abstract method");
            }
            return methods.getFirst().getReturnType();
        }
        throw new IllegalArgumentException("Newtype annotation only supports records and interfaces: " + element);
    }

    public static String getAccessorName(TypeElement element) {
        if (element.getKind() == ElementKind.RECORD) {
            return element.getRecordComponents().getFirst().getSimpleName() + "()";
        } else if (element.getKind() == ElementKind.INTERFACE) {
            List<ExecutableElement> methods = ElementFilter.methodsIn(element.getEnclosedElements()).stream()
                    .filter(m -> !m.isDefault() && !m.getModifiers().contains(Modifier.STATIC))
                    .toList();
            return methods.getFirst().getSimpleName() + "()";
        }
        throw new IllegalArgumentException("Newtype annotation only supports records and interfaces: " + element);
    }

    public static ClassName getImplClassName(TypeElement element) {
        ClassName publicClass = ClassName.get(element);
        if (element.getKind() == ElementKind.RECORD) {
            return publicClass;
        }
        return publicClass.peerClass(publicClass.simpleName() + "Impl");
    }
}
