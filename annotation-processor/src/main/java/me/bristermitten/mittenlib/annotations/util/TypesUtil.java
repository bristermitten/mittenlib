package me.bristermitten.mittenlib.annotations.util;

import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

/**
 * Utility class for working with {@link TypeMirror}s
 */
public class TypesUtil {
    private TypesUtil() {
    }

    /**
     * Get a "safe" version of a type.
     * This is defined as the boxed type for primitives, the erasure for parameterized types, otherwise simply the type itself
     * Examples:
     * <ul>
     * <li>{@code int -> Integer}</li>
     * <li>{@code Map<String, Integer> -> Map}</code></li>
     * <li>{@code String -> String}</li>
     * </ul>
     */
    public static TypeMirror getSafeType(Types types, TypeMirror typeMirror) {
        if (typeMirror.getKind().isPrimitive()) {
            return types.boxedClass((PrimitiveType) typeMirror).asType();
        }
        return types.erasure(typeMirror);
    }

    /**
     * Get a boxed version of a given type, if it is a primitive.
     * Otherwise, the type is returned unchanged
     */
    public static TypeMirror getBoxedType(Types types, TypeMirror typeMirror) {
        if (typeMirror.getKind().isPrimitive()) {
            return types.boxedClass((PrimitiveType) typeMirror).asType();
        }
        return typeMirror;
    }
}
