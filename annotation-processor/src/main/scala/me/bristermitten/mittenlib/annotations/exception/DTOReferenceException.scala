package me.bristermitten.mittenlib.annotations.exception

import javax.lang.model.element.Element
import javax.lang.model.`type`.TypeMirror
import me.bristermitten.mittenlib.annotations.compile.GeneratedTypeCache
import me.bristermitten.mittenlib.annotations.util.Stringify
import org.jspecify.annotations.Nullable
import scala.jdk.CollectionConverters.*

class DTOReferenceException(
    private val typeUsed: TypeMirror,
    private val typeCache: GeneratedTypeCache,
    @Nullable private val replaceWith: Class[?],
    @Nullable private val source: Element
) extends RuntimeException:

  override def getMessage: String =
    val rawTypes = typeCache.getByName(typeUsed.toString)
    val types =
      if (rawTypes == null) Nil
      else java.util.ArrayList(rawTypes).asScala.toList
    if (replaceWith == null && types.isEmpty) {
      s"Unknown type $typeUsed"
    } else {
      val typesReplaceWith =
        if (replaceWith != null) {
          replaceWith.getName
        } else if (types.size == 1) {
          Stringify.prettyStringify(types.head)
        } else {
          "any of " + types
            .map(Stringify.prettyStringify)
            .mkString("[", ", ", "]")
        }

      val location = Option(source)
        .map(Stringify.prettyStringify)
        .getOrElse("Unknown Location")

      s"""You seem to be using a generated type in a DTO.
         |This results in weird behaviour and so is not allowed.
         |You should replace $typeUsed with $typesReplaceWith.
         |This issue occurred in $location.
         |""".stripMargin
    }
