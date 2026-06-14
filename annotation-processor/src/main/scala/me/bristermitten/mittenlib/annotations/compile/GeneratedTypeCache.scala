package me.bristermitten.mittenlib.annotations.compile

import com.google.common.annotations.Beta
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.google.inject.Singleton
import java.util.Map
import java.util.Set
import java.util.stream.Collectors
import javax.lang.model.element.TypeElement
import scala.jdk.CollectionConverters.*

@Beta
@Singleton
class GeneratedTypeCache:
  private val generatedSpecs: BiMap[TypeElement, String] = HashBiMap.create()

  def getByName(name: String): Set[TypeElement] =
    generatedSpecs.entrySet().stream()
      .filter(entry => entry.getValue().contains(name) || name.contains(entry.getValue()))
      .map(entry => entry.getKey())
      .collect(Collectors.toSet())

  def put(source: TypeElement, name: String): Unit =
    generatedSpecs.put(source, name)
