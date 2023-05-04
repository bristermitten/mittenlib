package me.bristermitten.mittenlib.annotations.config

import com.squareup.javapoet.ClassName
import me.bristermitten.mittenlib.config.Config
import java.util.regex.Pattern
import javax.annotation.processing.ProcessingEnvironment
import javax.inject.Inject
import javax.lang.model.element.NestingKind
import javax.lang.model.element.TypeElement

/**
 * Responsible for generating proper class names for config classes
 */
class ConfigurationClassNameGenerator @Inject internal constructor(private val environment: ProcessingEnvironment) {
    /**
     * Generates a ClassName for the actual generated configuration class from a given DTO, using the package
     * of the given [TypeElement].
     *
     *
     * This returns an empty optional if the given type is not a DTO type, which is defined by the following rules:
     *
     *  1. The type must be annotated with [Config]
     *  1. The type must be a class
     *  1. The class name must end with "DTO" or "Config"
     *
     *
     *
     * The returned class name will be the same as the given type, but with the suffix removed.
     * It can also be manually specified in the Config annotation with [Config.className]
     *
     * @param configDTOType The DTO type
     * @return The generated ClassName
     */
    fun generateConfigurationClassName(configDTOType: TypeElement): ClassName? {
        if (configDTOType.nestingKind == NestingKind.MEMBER) {
            /*
            If the type is a nested class, then we first translate the enclosing class name (which may do nothing),
            then create a nested class name.
             */
            val enclosingElement = configDTOType.enclosingElement
            return generateConfigurationClassName(enclosingElement as TypeElement)
                ?.let { className ->
                    findConfigClassName(configDTOType)?.let { name -> className.nestedClass(name) }
                }
        }
        val packageName = environment.elementUtils.getPackageOf(configDTOType).qualifiedName.toString()
        return findConfigClassName(configDTOType)
            ?.let { ClassName.get(packageName, it) }
    }

    companion object {
        private val SUFFIX_PATTERN = Pattern.compile("(.+)(DTO|Config)")

        /**
         * Translates a [TypeElement] into its non-DTO name by
         * reading a [Config.className] or removing the suffix.
         *
         * @param dtoType the DTO type
         * @return the non-DTO name, if possible. An empty optional implies that the type is not a valid DTO.
         */
        private fun findConfigClassName(dtoType: TypeElement): String? {
            val annotation = dtoType.getAnnotation(
                Config::class.java
            )
            if (annotation != null && annotation.className.isNotEmpty()) {
                return (annotation.className)
            }
            val matcher = SUFFIX_PATTERN.matcher(dtoType.simpleName)
            return if (matcher.find()) {
                matcher.group(1)
            } else null
        }
    }
}
