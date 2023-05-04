package me.bristermitten.mittenlib.annotations.config

import com.squareup.javapoet.ClassName

data class ConfigClassBuildSettings(val generatedClassName: ClassName, val generateRecord: Boolean)
