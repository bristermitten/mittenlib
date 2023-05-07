package me.bristermitten.mittenlib.annotations.config.stage

import com.squareup.javapoet.TypeSpec
import me.bristermitten.mittenlib.annotations.config.ConfigClassBuildSettings
import me.bristermitten.mittenlib.annotations.config.ConfigurationClassNameGenerator
import javax.inject.Inject
import javax.lang.model.element.TypeElement

class ExtractSettingsBuildStage @Inject internal constructor(private val classNameGenerator: ConfigurationClassNameGenerator) :
    BuildStage<Unit, ConfigClassBuildSettings> {
    override fun name(): String {
        return "extract-settings"
    }

    override fun apply(generateFrom: TypeElement, builder: TypeSpec.Builder?, input: Unit): ConfigClassBuildSettings {
        val className = classNameGenerator.generateConfigurationClassName(generateFrom)
            ?: throw IllegalArgumentException("Cannot determine name for @Config class " + generateFrom.qualifiedName)

        TODO()
    }
}
