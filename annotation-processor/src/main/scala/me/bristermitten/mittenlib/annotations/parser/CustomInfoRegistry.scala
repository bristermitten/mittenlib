package me.bristermitten.mittenlib.annotations.parser

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.palantir.javapoet.TypeName
import java.util.Optional
import javax.lang.model.`type`.TypeMirror

abstract class CustomInfoRegistry[T]:
  private val infoMultimap: Multimap[TypeName, T] = HashMultimap.create()

  def register(clazz: TypeName, info: T): Unit =
    infoMultimap.put(clazz, info)

  def getCustomInfo(propertyType: TypeMirror): Optional[T] =
    val fromMap = infoMultimap.get(TypeName.get(propertyType))
    if (fromMap.isEmpty) {
      Optional.empty()
    } else if (fromMap.size() > 1) {
      throw new IllegalArgumentException("Not sure how to handle multiple yet")
    } else {
      Optional.of(fromMap.iterator().next())
    }
