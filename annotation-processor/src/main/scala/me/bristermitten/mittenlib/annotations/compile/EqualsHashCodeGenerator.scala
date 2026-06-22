package me.bristermitten.mittenlib.annotations.compile

import com.google.inject.Inject
import com.palantir.javapoet.{ArrayTypeName, ClassName, MethodSpec, TypeName}

import java.util.{List as JList, Objects as JObjects}
import javax.lang.model.element.Modifier
import me.bristermitten.mittenlib.annotations.ast.Property
import me.bristermitten.mittenlib.annotations.domain.{
  Property => DomainProperty
}

import scala.jdk.CollectionConverters.*
import _root_.me.bristermitten.mittenlib.codegen.dsl.*
import _root_.me.bristermitten.mittenlib.codegen.dsl.BlockBuilder.*
import _root_.me.bristermitten.mittenlib.codegen.dsl.given

/** Generates equals and hashCode methods for configuration classes. This class
  * creates standard implementations that compare all properties of a
  * configuration class for equality and generate consistent hash codes.
  */
class EqualsHashCodeGenerator @Inject() (private val methodNames: MethodNames):

  private def toSharedFields(properties: JList[Property]): List[SharedField] =
    properties.asScala.toList.map { p =>
      val t = TypeRef.of(TypeName.get(p.propertyType()))
      val isArr = TypeName.get(p.propertyType()).isInstanceOf[ArrayTypeName]
      SharedField(
        name = p.name(),
        tpe = t,
        accessor = receiver => receiver.call(methodNames.safeMethodName(p)),
        isArray = isArr
      )
    }

  private def toSharedFieldsDomain(
      properties: JList[DomainProperty]
  ): List[SharedField] =
    properties.asScala.toList.map { p =>
      val t = TypeRef.of(TypeName.get(p.typeMirror))
      val isArr = TypeName.get(p.typeMirror).isInstanceOf[ArrayTypeName]
      SharedField(
        name = p.name,
        tpe = t,
        accessor = receiver => receiver.call(methodNames.safeMethodName(p)),
        isArray = isArr
      )
    }

  /** Generates an equals method for a configuration class.
    */
  def generateEquals(
      configClassName: ClassName,
      properties: JList[Property]
  ): MethodSpec =
    val methodDecl =
      BoilerplateHelper.equalsDecl(configClassName, toSharedFields(properties))
    CodeBlockRenderer.renderMethod(methodDecl)

  /** Generates an equals method for a configuration class (domain.Property
    * variant).
    */
  def generateEqualsDomain(
      configClassName: ClassName,
      properties: JList[DomainProperty]
  ): MethodSpec =
    val methodDecl =
      BoilerplateHelper.equalsDecl(
        configClassName,
        toSharedFieldsDomain(properties)
      )
    CodeBlockRenderer.renderMethod(methodDecl)

  /** Generates a hashCode method for a configuration class.
    */
  def generateHashCode(properties: JList[Property]): MethodSpec =
    val methodDecl = BoilerplateHelper.hashCodeDecl(toSharedFields(properties))
    CodeBlockRenderer.renderMethod(methodDecl)

  /** Generates a hashCode method for a configuration class (domain.Property
    * variant).
    */
  def generateHashCodeDomain(properties: JList[DomainProperty]): MethodSpec =
    val methodDecl =
      BoilerplateHelper.hashCodeDecl(toSharedFieldsDomain(properties))
    CodeBlockRenderer.renderMethod(methodDecl)
