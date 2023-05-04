package me.bristermitten.mittenlib.annotations.config

import com.google.common.annotations.Beta
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import me.bristermitten.mittenlib.config.GeneratedConfig
import javax.inject.Singleton
import javax.lang.model.element.TypeElement

/**
 * **This class should not be treated as a public API, and is subject to change at any time.**
 *
 * A cache of generated types.
 * This class is a pretty big hack to be honest, but it allows us to generate pretty error messages
 * when people accidentally use the generated type names in a DTO class.
 * This class maintains a [com.google.common.collect.BiMap] between the DTO [javax.lang.model.element.TypeElement]s and the fully qualified name
 * of the configuration class, which means that we can easily get the DTO class from the configuration class
 * to generate a nice error message.
 *
 *
 * Unfortunately, because the referenced class might not exist as an actual class yet, we can't always
 * use the [GeneratedConfig.source] property, so this class exists as a workaround.
 */
@Beta
@Singleton
class GeneratedTypeCache {
    /**
     * @return the underlying mapping of source type to generated type's qualified name
     * This type is mutable and should be modified with care
     */
    /**
     * Map of source type to generated type's qualified name
     */
    val generatedSpecs: BiMap<TypeElement, String> = HashBiMap.create()

    /**
     * Gets all the known DTO classes which generate a configuration class with the given qualified name
     *
     * @param name the qualified name of the configuration class
     * @return a set of all the DTO classes which generate the given configuration class
     */
    fun getByName(name: String): Set<TypeElement> {
        return generatedSpecs.entries
            .filter { (_, value): Map.Entry<TypeElement?, String?> ->
                value!!.contains(name) || name.contains(
                    value
                )
            }
            .map { it.key }
            .toSet()
    }
}
