package me.bristermitten.mittenlib.annotations.ast

import javax.lang.model.element.TypeElement

case class CustomDeserializerInfo(
    deserializerClass: TypeElement,
    isStatic: Boolean,
    isFallback: Boolean,
    isGlobal: Boolean
)
