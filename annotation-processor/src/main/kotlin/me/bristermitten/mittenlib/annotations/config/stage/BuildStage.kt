package me.bristermitten.mittenlib.annotations.config.stage

import javax.lang.model.element.TypeElement

/**
 * A Stage in the process of building a config class
 */
interface BuildStage<I, O> {
    fun name(): String
    fun apply(generateFrom: TypeElement, input: I): O
}
