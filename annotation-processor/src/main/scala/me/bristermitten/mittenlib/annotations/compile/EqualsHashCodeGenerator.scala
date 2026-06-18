package me.bristermitten.mittenlib.annotations.compile

import com.google.inject.Inject
import com.palantir.javapoet.ClassName
import com.palantir.javapoet.MethodSpec
import java.util.{List => JList, Objects => JObjects}
import javax.lang.model.element.Modifier
import me.bristermitten.mittenlib.annotations.ast.Property
import scala.jdk.CollectionConverters.*

import _root_.me.bristermitten.mittenlib.codegen.dsl.*
import _root_.me.bristermitten.mittenlib.codegen.dsl.BlockBuilder.*
import _root_.me.bristermitten.mittenlib.codegen.dsl.given

/**
 * Generates equals and hashCode methods for configuration classes. This class creates standard
 * implementations that compare all properties of a configuration class for equality and generate
 * consistent hash codes.
 */
class EqualsHashCodeGenerator @Inject() (
  private val methodNames: MethodNames
):

  private def toSharedFields(properties: JList[Property]): List[SharedField] =
    properties.asScala.toList.map { p =>
      val t = TypeRef.of(com.palantir.javapoet.TypeName.get(p.propertyType()))
      val isArr = com.palantir.javapoet.TypeName.get(p.propertyType()).isInstanceOf[com.palantir.javapoet.ArrayTypeName]
      SharedField(
        name = p.name(),
        tpe = t,
        accessor = receiver => receiver.call(methodNames.safeMethodName(p)),
        isArray = isArr
      )
    }

  /**
   * Generates an equals method for a configuration class.
   */
  def generateEquals(configClassName: ClassName, properties: JList[Property]): MethodSpec =
    val methodDecl = BoilerplateHelper.equalsDecl(configClassName, toSharedFields(properties))
    CodeBlockRenderer.renderMethod(methodDecl)

  /**
   * Generates a hashCode method for a configuration class.
   */
  def generateHashCode(properties: JList[Property]): MethodSpec =
    val methodDecl = BoilerplateHelper.hashCodeDecl(toSharedFields(properties))
    CodeBlockRenderer.renderMethod(methodDecl)

