package me.bristermitten.mittenlib.annotations.util;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import me.bristermitten.mittenlib.annotations.compile.GeneratedTypeCache;
import me.bristermitten.mittenlib.annotations.exception.DTOReferenceException;
import me.bristermitten.mittenlib.config.Config;
import me.bristermitten.mittenlib.config.generate.CascadeToInnerClasses;
import me.bristermitten.mittenlib.config.tree.DataTree;
import org.jspecify.annotations.Nullable;

import javax.inject.Inject;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Helper class for working with {@link TypeMirror}s
 */
public class TypesUtil {
    private final Types types;

    private final GeneratedTypeCache generatedTypeCache;

    @Inject
    TypesUtil(Types types, GeneratedTypeCache generatedTypeCache) {
        this.types = types;
        this.generatedTypeCache = generatedTypeCache;
    }

    /**
     * Get a "safe" version of a type, where "safe" refers to being able to use it as the target of a <code>instanceof</code> check without compilation errors
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
    public boolean isNullable(Element element) {
        if (element instanceof VariableElement variableElement) {
            if (isNullable(variableElement.asType())) {
                return true;
            }
        }
        if (element instanceof ExecutableElement exec) {
            if (isNullable(exec.getReturnType())) {
                return true;
            }
        }
        for (AnnotationMirror ann : element.getAnnotationMirrors()) {
            if (ann.getAnnotationType().asElement().getSimpleName().toString().equals("Nullable")) {
                return true;
            }
        }
        return false;
    }

    public boolean isNullable(TypeMirror typeMirror) {
        if (typeMirror.getKind().isPrimitive()) {
            return false; // primitives are never nullable
        }
        for (AnnotationMirror annotationMirror : typeMirror.getAnnotationMirrors()) {
            if (annotationMirror.getAnnotationType().asElement().getSimpleName().toString().equals("Nullable")) {
                return true;
            }
        }
        return false; // no Nullable annotation found
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
        //noinspection ConstantValue I don't know why intellij thinks getAnnotation can never return null
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


    /**
     * Checks if a type is a config type (annotated with @Config).
     *
     * @param mirror The type to check
     * @return true if the type is a config type, false otherwise
     */
    public boolean isConfigType(TypeMirror mirror) {
        if (mirror.getKind() == TypeKind.ERROR) {
            throw new DTOReferenceException(mirror, generatedTypeCache, null, null);
        }
        return mirror instanceof DeclaredType declaredType &&
                getAnnotation(declaredType.asElement(), Config.class) != null;
    }

    public Optional<TypeName> getDataTreeType(TypeName type) {
        type = type.isBoxedPrimitive() ? type.unbox() : type;
        if (type.equals(TypeName.INT) || type.equals(TypeName.LONG) || type.equals(TypeName.SHORT) || type.equals(TypeName.BYTE)) {
            return Optional.of(ClassName.get(DataTree.DataTreeLiteral.DataTreeLiteralInt.class));
        }
        if (type.equals(TypeName.FLOAT) || type.equals(TypeName.DOUBLE)) {
            return Optional.of(ClassName.get(DataTree.DataTreeLiteral.DataTreeLiteralFloat.class));
        }
        if (type.equals(TypeName.BOOLEAN)) {
            return Optional.of(ClassName.get(DataTree.DataTreeLiteral.DataTreeLiteralBoolean.class));
        }
        if (type.equals(ClassName.get(String.class))) {
            return Optional.of(ClassName.get(DataTree.DataTreeLiteral.DataTreeLiteralString.class));
        }
        if (type instanceof ParameterizedTypeName p) {
            if (p.rawType.equals(ClassName.get(Map.class))) {
                return Optional.of(ClassName.get(DataTree.DataTreeMap.class));
            }
            if (p.rawType.equals(ClassName.get(List.class))) {
                return Optional.of(ClassName.get(DataTree.DataTreeArray.class));
            }
        }
        return Optional.empty();
    }
}
