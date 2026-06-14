package me.bristermitten.mittenlib.annotations.compile

import com.google.inject.Inject
import com.palantir.javapoet.ClassName
import com.palantir.javapoet.MethodSpec
import java.util.{List => JList}
import javax.lang.model.element.Modifier
import me.bristermitten.mittenlib.annotations.ast.Property
import scala.jdk.CollectionConverters.*

import _root_.me.bristermitten.mittenlib.codegen.dsl.*
import _root_.me.bristermitten.mittenlib.codegen.dsl.BlockBuilder.*
import _root_.me.bristermitten.mittenlib.codegen.dsl.given

/**
 * Generates toString methods for configuration classes. This class creates a standard toString
 * implementation that includes all properties of a configuration class in a readable format.
 */
class ToStringGenerator @Inject() ():

  /**
   * Generates a toString method for a configuration class.
   */
  def generateToString(properties: JList[Property], className: ClassName): MethodSpec =
    val propsList = properties.asScala.toList
    var expr: Expr = Expr.str(s"${className.simpleName()}{")

    for ((fieldSpec, idx) <- propsList.zipWithIndex) {
      expr = Expr.BinaryOp(expr, "+", Expr.str(s"${fieldSpec.name()}="))
      expr = Expr.BinaryOp(expr, "+", Expr.This.call(fieldSpec.name()))
      if (idx != propsList.size - 1) {
        expr = Expr.BinaryOp(expr, "+", Expr.str(","))
      }
    }
    expr = Expr.BinaryOp(expr, "+", Expr.str("}"))

    val methodDecl = MethodDecl.build(
      name = "toString",
      returnType = Types.String,
      parameters = Nil,
      modifiers = List(Modifier.PUBLIC)
    ) {
      return_(expr)
    }

    CodeBlockRenderer.renderMethod(methodDecl)

