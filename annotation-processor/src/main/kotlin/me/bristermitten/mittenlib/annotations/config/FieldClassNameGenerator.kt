package me.bristermitten.mittenlib.annotations.config

import me.bristermitten.mittenlib.annotations.util.TypesUtil
import me.bristermitten.mittenlib.config.names.ConfigName
import me.bristermitten.mittenlib.config.names.NamingPattern
import me.bristermitten.mittenlib.config.names.NamingPatternTransformer
import javax.inject.Inject
import javax.lang.model.element.VariableElement

/**
 * Responsible for generating serial keys based on DTO fields
 */
class FieldClassNameGenerator @Inject internal constructor(private val typesUtil: TypesUtil) {
    /**
     * Get a suitable serialization key for a given DTO field.
     * This is the String that is looked up from the given [DeserializationContext.getData]
     *
     *
     * This method takes into account a number of things:
     * 1. A [ConfigName] annotation, if present.
     * 2. A [NamingPattern] annotation, if present.
     * 3. The name of the field itself
     *
     *
     * The first match from this list is returned as the key.
     *
     * @param element The DTO field
     * @return The key to use when reading from [DeserializationContext.getData] for the given field.
     */
    fun getConfigFieldName(element: VariableElement): String {
        val name = element.getAnnotation(ConfigName::class.java)
        if (name != null) {
            return name.value
        }
        val annotation = typesUtil.getAnnotation(element, NamingPattern::class.java)
        return if (annotation != null) {
            NamingPatternTransformer.format(
                element.simpleName.toString(), annotation.value
            )
        } else element.simpleName.toString()
    }
}
