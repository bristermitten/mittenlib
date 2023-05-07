package me.bristermitten.mittenlib.annotations.config.stage

import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.TypeElement

/**
 * A Stage in the process of building a config class
 */
interface BuildStage<I, O> {
    fun name(): String
    fun apply(generateFrom: TypeElement, builder: TypeSpec.Builder?, input: I): O
}
