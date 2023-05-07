package me.bristermitten.mittenlib.annotations.util

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import me.bristermitten.mittenlib.annotations.config.ConfigClassBuilder
import kotlin.reflect.KClass

fun KClass<*>.asTypeName(): ClassName = ClassName.get(this.java)

fun ClassName.withTypeArguments(vararg typeArguments: TypeName): ParameterizedTypeName =
    ParameterizedTypeName.get(this, *typeArguments)

/**
 * Gets an appropriate name for a generated `deserialize` method for the given [TypeName].
 * `getDeserializeMethodName("Foo")` = `deserializeFoo`
 * `getDeserializeMethodName("Foo.Bar")` = `deserializeFooBar`
 */
fun getDeserializeMethodName(name: TypeName): String {
    return if (name is ClassName) {
        ConfigClassBuilder.DESERIALIZE + name.simpleNames().joinToString("")
    } else ConfigClassBuilder.DESERIALIZE + name
}
