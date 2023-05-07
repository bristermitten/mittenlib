package me.bristermitten.mittenlib.annotations.config

import com.squareup.javapoet.ClassName
import javax.lang.model.element.VariableElement

data class ConfigClassBuildSettings(
    val generatedClassName: ClassName,
    val generateRecord: Boolean,
    val fields: List<VariableElement>
)
