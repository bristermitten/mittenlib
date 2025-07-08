package me.bristermitten.mittenlib.annotations.ast;

import javax.lang.model.element.TypeElement;

/**
 * @param deserializerClass the class where the deserialization function is held
 * @param isStatic          whether the method is static
 * @param isFallback        whether the deserializer is a fallback
 * @param isGlobal          whether the deserializer is globally applied
 */
public record CustomDeserializerInfo(
        TypeElement deserializerClass,
        boolean isStatic,
        boolean isFallback,
        boolean isGlobal
) {
}
