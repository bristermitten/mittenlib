package me.bristermitten.mittenlib.annotations.compile

import com.google.inject.Singleton
import com.palantir.javapoet.ClassName
import io.toolisticon.aptk.tools.TypeMirrorWrapper
import me.bristermitten.mittenlib.annotations.domain.ConfigStructure

import java.util.HashMap
import javax.lang.model.`type`.{TypeKind, TypeMirror}

@Singleton
class ConfigNameCache:
  private val cache = new HashMap[ClassName, ConfigStructure]()

  def put(structure: ConfigStructure): Unit =
    cache.put(structure.name, structure)

  def putDomain(structure: ConfigStructure): Unit =
    put(structure)

  def lookupDomain(name: ClassName): Option[ConfigStructure] =
    Option(cache.get(name))

  def lookupDomain(mirror: TypeMirror): Option[ConfigStructure] =
    if (mirror.getKind != TypeKind.DECLARED) {
      None
    } else {
      lookupDomain(
        ClassName.bestGuess(TypeMirrorWrapper.wrap(mirror).getQualifiedName)
      )
    }

  def clear(): Unit =
    cache.clear()
