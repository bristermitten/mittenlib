package me.bristermitten.mittenlib.annotations.ast

import javax.lang.model.element.TypeElement

case class CustomSerializerInfo(
    serializerClass: TypeElement,
    isStatic: Boolean
)
