package me.bristermitten.mittenlib.annotations.config.stage

import me.bristermitten.mittenlib.annotations.config.ConfigClassBuildSettings
import me.bristermitten.mittenlib.annotations.config.ConfigurationClassNameGenerator
import me.bristermitten.mittenlib.annotations.util.ElementsFinder
import me.bristermitten.mittenlib.annotations.util.TypesUtil
import me.bristermitten.mittenlib.config.generate.GenerateRecord
import javax.inject.Inject
import javax.lang.model.element.TypeElement

class ExtractSettingsBuildStage @Inject internal constructor(
    private val classNameGenerator: ConfigurationClassNameGenerator,
    private val typesUtil: TypesUtil,
    private val elementsFinder: ElementsFinder
) :
    BuildStage<Unit, ConfigClassBuildSettings> {
    override fun name(): String {
        return "extract-settings"
    }

    override fun apply(generateFrom: TypeElement, input: Unit): ConfigClassBuildSettings {
        val className = classNameGenerator.generateConfigurationClassName(generateFrom)
            ?: throw IllegalArgumentException("Cannot determine name for @Config class " + generateFrom.qualifiedName)

        val useRecords = typesUtil.getAnnotation<GenerateRecord>(generateFrom)?.value ?: false

        val fields = elementsFinder.getApplicableVariableElements(generateFrom)
        return ConfigClassBuildSettings(className, useRecords, fields)
    }
}
