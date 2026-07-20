package me.bristermitten.mittenlib.annotations.compile

import com.google.inject.Singleton
import com.palantir.javapoet.ClassName
import io.toolisticon.aptk.tools.TypeMirrorWrapper
import me.bristermitten.mittenlib.annotations.ast.AbstractConfigStructure
import me.bristermitten.mittenlib.annotations.domain

import java.util
import java.util.{HashMap, Optional}
import javax.lang.model.`type`.{TypeKind, TypeMirror}

@Singleton
class ConfigNameCache:
  private val astCache = new util.HashMap[ClassName, AbstractConfigStructure]()
  private val domainCache =
    new util.HashMap[ClassName, domain.ConfigStructure]()

  def lookupAST(name: ClassName): Optional[AbstractConfigStructure] =
    Optional.ofNullable(astCache.get(name))

  def lookupAST(mirror: TypeMirror): Optional[AbstractConfigStructure] =
    if (mirror.getKind != TypeKind.DECLARED) {
      Optional.empty()
    } else {
      lookupAST(
        ClassName.bestGuess(TypeMirrorWrapper.wrap(mirror).getQualifiedName)
      )
    }

  def put(ast: AbstractConfigStructure): Unit =
    astCache.put(ast.name(), ast)

  def putDomain(
      ast: me.bristermitten.mittenlib.annotations.domain.ConfigStructure
  ): Unit =
    domainCache.put(ast.name, ast)

  def lookupDomain(name: ClassName): Option[domain.ConfigStructure] =
    Option(domainCache.get(name))
