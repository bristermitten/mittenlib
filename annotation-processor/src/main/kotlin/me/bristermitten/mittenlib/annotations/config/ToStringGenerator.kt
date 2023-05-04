package me.bristermitten.mittenlib.annotations.config

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

class ToStringGenerator {
    fun generateToString(
        typeSpecBuilder: TypeSpec.Builder,
        className: ClassName?
    ): MethodSpec {
        val builder = MethodSpec.methodBuilder("toString")
            .addAnnotation(Override::class.java)
            .addModifiers(Modifier.PUBLIC)
            .returns(String::class.java)
        val code = CodeBlock.builder()
        code.add("return \"\$T{\"", className)
        val fieldSpecs = typeSpecBuilder.fieldSpecs
        for (i in fieldSpecs.indices) {
            val fieldSpec = fieldSpecs[i]
            code.add(" + \"\$L=\" + \$N ", fieldSpec.name, fieldSpec.name)
            if (i != fieldSpecs.size - 1) {
                code.add("+ \",\"")
            }
        }
        code.add("+ \"}\"")
        builder.addStatement(code.build())
        return builder.build()
    }
}
