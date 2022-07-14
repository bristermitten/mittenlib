package me.bristermitten.mittenlib.annotations.util;

import javax.inject.Inject;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

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
}
