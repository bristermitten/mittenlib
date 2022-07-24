package me.bristermitten.mittenlib.annotations.util;

import me.bristermitten.mittenlib.config.generate.CascadeToInnerClasses;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.lang.annotation.Annotation;

/**
 * Helper class for working with {@link TypeMirror}s
 */
public class TypesUtil {
    private final Types types;

    @Inject
    TypesUtil(Types types) {
        this.types = types;
    }

    /**
     * Get a "safe" version of a type.
     * This is defined as the boxed type for primitives, the erasure for parameterized types, otherwise simply the type itself
     * Examples:
     * <ul>
     * <li>{@code int -> Integer}</li>
     * <li>{@code Map<String, Integer> -> Map}</li>
     * <li>{@code String -> String}</li>
     * </ul>
     */
    public TypeMirror getSafeType(TypeMirror typeMirror) {
        if (typeMirror.getKind().isPrimitive()) {
            return types.boxedClass((PrimitiveType) typeMirror).asType();
        }
        return types.erasure(typeMirror);
    }

    /**
     * Get a boxed version of a given type, if it is a primitive.
     * Otherwise, the type is returned unchanged
     */
    public TypeMirror getBoxedType(TypeMirror typeMirror) {
        if (typeMirror.getKind().isPrimitive()) {
            return types.boxedClass((PrimitiveType) typeMirror).asType();
        }
        return typeMirror;
    }

    /**
     * Return if a {@link VariableElement} should be considered nullable or not
     * Everything is considered non-nullable unless it is specifically annotated as nullable.
     * Any annotation named "Nullable" is supported, i.e. jetbrains or javax
     *
     * @param element The element to check
     * @return True if the element is nullable, false otherwise
     */
    public boolean isNullable(VariableElement element) {
        for (AnnotationMirror ann : element.getAnnotationMirrors()) {
            if (ann.getAnnotationType().asElement().getSimpleName().toString().equals("Nullable")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets an {@link Annotation} present on an {@link Element}, if present.
     * This method is slightly different to {@link Element#getAnnotation(Class)},
     * in that it respects the semantics described in {@link CascadeToInnerClasses}
     *
     * @param e    The element
     * @param type The class of the annotation
     * @param <A>  The annotation type
     * @return The annotation value, if present, else null
     */
    public <A extends Annotation> @Nullable A getAnnotation(Element e, Class<A> type) {
        A onElem = e.getAnnotation(type);
        if (onElem != null) {
            return onElem;
        }

        if (type.getAnnotation(CascadeToInnerClasses.class) != null) {
            var enclosing = e.getEnclosingElement();
            if (enclosing == null) {
                return null;
            }
            return getAnnotation(enclosing, type);
        }
        return null;
    }
}
