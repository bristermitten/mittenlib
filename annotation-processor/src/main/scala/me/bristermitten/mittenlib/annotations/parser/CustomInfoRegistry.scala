package me.bristermitten.mittenlib.annotations.parser

import com.google.common.collect.{HashMultimap, Multimap}
import com.palantir.javapoet.TypeName
import io.toolisticon.aptk.tools.MessagerUtils
import javax.lang.model.`type`.TypeMirror
import scala.jdk.CollectionConverters.*

abstract class CustomInfoRegistry[T]:
  private val infoMultimap: Multimap[TypeName, T] = HashMultimap.create()

  def register(clazz: TypeName, info: T): Unit =
    infoMultimap.put(clazz, info)

  def getCustomInfo(propertyType: TypeMirror): Option[T] =
    val fromMap = infoMultimap.get(TypeName.get(propertyType))
    if (fromMap.isEmpty) {
      None
    } else if (fromMap.size() > 1) {
      MessagerUtils.error(
        null: javax.lang.model.element.Element,
        s"Multiple custom registrations found for type $propertyType"
      )
      Some(fromMap.iterator().next())
    } else {
      Some(fromMap.iterator().next())
    }

  def allInfos: Map[TypeName, List[T]] =
    infoMultimap
      .asMap()
      .asScala
      .map { case (k, v) =>
        k -> v.asScala.toList
      }
      .toMap
