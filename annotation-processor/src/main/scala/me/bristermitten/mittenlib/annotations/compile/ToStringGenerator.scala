package me.bristermitten.mittenlib.annotations.compile

import com.google.inject.Inject
import com.palantir.javapoet.ClassName
import com.palantir.javapoet.MethodSpec
import java.util.{List => JList}
import javax.lang.model.element.Modifier
import me.bristermitten.mittenlib.annotations.ast.Property
import me.bristermitten.mittenlib.annotations.domain.{
  Property => DomainProperty
}
import scala.jdk.CollectionConverters.*

import _root_.me.bristermitten.mittenlib.codegen.dsl.*
import _root_.me.bristermitten.mittenlib.codegen.dsl.BlockBuilder.*
import _root_.me.bristermitten.mittenlib.codegen.dsl.given

/** Generates toString methods for configuration classes. This class creates a
  * standard toString implementation that includes all properties of a
  * configuration class in a readable format.
  */
class ToStringGenerator @Inject() ():

  /** Generates a toString method for a configuration class.
    */
  def generateToString(
      properties: JList[Property],
      className: ClassName
  ): MethodSpec =
    val fields = properties.asScala.toList.map { p =>
      val t = TypeRef.of(com.palantir.javapoet.TypeName.get(p.propertyType()))
      val isArr = com.palantir.javapoet.TypeName
        .get(p.propertyType())
        .isInstanceOf[com.palantir.javapoet.ArrayTypeName]
      SharedField(
        name = p.name(),
        tpe = t,
        accessor = receiver =>
          if (receiver == Expr.This) Var(p.name(), t)
          else receiver.field(p.name()),
        isArray = isArr
      )
    }

    val methodDecl = BoilerplateHelper.toStringDecl(className, fields, ",")
    CodeBlockRenderer.renderMethod(methodDecl)

  /** Generates a toString method for a configuration class (domain.Property
    * variant).
    */
  def generateToStringDomain(
      properties: JList[DomainProperty],
      className: ClassName
  ): MethodSpec =
    val fields = properties.asScala.toList.map { p =>
      val t = TypeRef.of(com.palantir.javapoet.TypeName.get(p.typeMirror))
      val isArr = com.palantir.javapoet.TypeName
        .get(p.typeMirror)
        .isInstanceOf[com.palantir.javapoet.ArrayTypeName]
      SharedField(
        name = p.name,
        tpe = t,
        accessor = receiver =>
          if (receiver == Expr.This) Var(p.name, t)
          else receiver.field(p.name),
        isArray = isArr
      )
    }

    val methodDecl = BoilerplateHelper.toStringDecl(className, fields, ",")
    CodeBlockRenderer.renderMethod(methodDecl)
