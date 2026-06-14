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

  /**
   * Generates an equals method for a configuration class.
   */
  def generateEquals(configClassName: ClassName, properties: JList[Property]): MethodSpec =
    val context = StagedExpr.param[Any](Types.Object, "o")
    val methodDecl = MethodDecl.build(
      name = "equals",
      returnType = Types.Boolean,
      parameters = List(context.asVar),
      modifiers = List(Modifier.PUBLIC)
    ) {
      ifThen(Expr.This === context) {
        return_(Expr.bool(true))
      }
      ifThen(context.isNull || (Expr.This.call("getClass") !== context.call("getClass"))) {
        return_(Expr.bool(false))
      }
      val thatType = TypeRef.of(configClassName)
      val that = declare(thatType, "that", context.cast(thatType))

      for (property <- properties.asScala) {
        val safeName = methodNames.safeMethodName(property)
        ifThen(!Expr.staticCall(TypeRef.of(classOf[JObjects]), "equals", Expr.This.call(safeName), that.call(safeName))) {
          return_(Expr.bool(false))
        }
      }
      return_(Expr.bool(true))
    }

    CodeBlockRenderer.renderMethod(methodDecl)

  /**
   * Generates a hashCode method for a configuration class.
   */
  def generateHashCode(properties: JList[Property]): MethodSpec =
    val methodDecl = MethodDecl.build(
      name = "hashCode",
      returnType = Types.Int,
      parameters = Nil,
      modifiers = List(Modifier.PUBLIC)
    ) {
      val args = properties.asScala.map(p => Expr.This.call(methodNames.safeMethodName(p))).toList
      return_(Expr.staticCall(TypeRef.of(classOf[JObjects]), "hash", args *))
    }

    CodeBlockRenderer.renderMethod(methodDecl)

