package me.bristermitten.mittenlib.annotations.exception

import me.bristermitten.mittenlib.annotations.config.GeneratedTypeCache
import me.bristermitten.mittenlib.annotations.util.toPrettyString
import javax.lang.model.element.Element
import javax.lang.model.type.TypeMirror

/**
 * Thrown when a DTO class references an invalid type
 */
class DTOReferenceException
/**
 * Create a new DTOReferenceException
 *
 * @param typeUsed    The invalid type that was uses
 * @param typeCache   The type cache, used for generating the error message
 * @param replaceWith The type to replace the invalid type with, if known
 * @param source      The source element (i.e. the element referencing the invalid type), if known
 */(
    @field:Transient private val typeUsed: TypeMirror?,
    @field:Transient private val typeCache: GeneratedTypeCache,
    private val replaceWith: Class<*>?,
    @field:Transient private val source: Element?
) : RuntimeException() {
    override val message: String
        get() {
            val typesReplaceWith = if (replaceWith != null) {
                replaceWith.name
            } else {
                val types = typeCache.getByName(typeUsed.toString())
                if (types.isEmpty()) {
                    return "Unknown type $typeUsed"
                }
                if (types.size == 1) types.single().toPrettyString() else "any of " + types.map { it.toPrettyString() }
            }
            return """
                You seem to be using a generated type in a DTO.
                This results in weird behaviour and so is not allowed.
                You should replace $typeUsed with $typesReplaceWith.
                This issue occurred in ${source?.toPrettyString() ?: "<Unknown Location>"}.
                
                """
                .trimIndent()
        }
}
