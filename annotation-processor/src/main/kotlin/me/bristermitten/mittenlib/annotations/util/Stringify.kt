package me.bristermitten.mittenlib.annotations.util

import com.squareup.javapoet.JavaFile
import javax.lang.model.element.Element
import javax.lang.model.element.VariableElement

object Stringify {
    fun stringify(javaFile: JavaFile): String {
        return javaFile.packageName + "." + javaFile.typeSpec.name
    }
}

fun Element.toPrettyString(): String {
    if (this !is VariableElement) return this.toString()

    return "${this.asType()} $simpleName in class ${enclosingElement.toPrettyString()}"

}
