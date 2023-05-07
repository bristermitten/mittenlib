package me.bristermitten.mittenlib.annotations.util

import me.bristermitten.mittenlib.annotations.config.ConfigClassBuildSettings
import me.bristermitten.mittenlib.config.generate.GenerateRecord
import javax.lang.model.element.TypeElement

fun TypesUtil.shouldGenerateRecord(element: TypeElement, defaults: ConfigClassBuildSettings) =
    getAnnotation<GenerateRecord>(element)?.value ?: defaults.generateRecords