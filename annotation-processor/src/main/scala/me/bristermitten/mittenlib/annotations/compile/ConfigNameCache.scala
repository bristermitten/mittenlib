package me.bristermitten.mittenlib.annotations.compile

import com.google.inject.Singleton
import com.palantir.javapoet.ClassName
import io.toolisticon.aptk.tools.TypeMirrorWrapper
import java.util.HashMap
import java.util.Map
import java.util.Optional
import javax.lang.model.`type`.TypeKind
import javax.lang.model.`type`.TypeMirror
import me.bristermitten.mittenlib.annotations.ast.AbstractConfigStructure

@Singleton
class ConfigNameCache:
  private val astCache = new HashMap[ClassName, AbstractConfigStructure]()

  def lookupAST(name: ClassName): Optional[AbstractConfigStructure] =
    Optional.ofNullable(astCache.get(name))

  def lookupAST(mirror: TypeMirror): Optional[AbstractConfigStructure] =
    if (mirror.getKind() != TypeKind.DECLARED) {
      Optional.empty()
    } else {
      lookupAST(ClassName.bestGuess(TypeMirrorWrapper.wrap(mirror).getQualifiedName()))
    }

  def put(ast: AbstractConfigStructure): Unit =
    astCache.put(ast.name(), ast)
