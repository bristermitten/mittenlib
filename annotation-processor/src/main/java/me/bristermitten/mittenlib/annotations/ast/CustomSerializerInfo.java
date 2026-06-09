package me.bristermitten.mittenlib.annotations.ast;

import javax.lang.model.element.TypeElement;

/**
 * Information about a custom serializer.
 *
 * @param serializerClass the class where the serialization function is held
 * @param isStatic whether the method is static
 */
public record CustomSerializerInfo(TypeElement serializerClass, boolean isStatic) {}
