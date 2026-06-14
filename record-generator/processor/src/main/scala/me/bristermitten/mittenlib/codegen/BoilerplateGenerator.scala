package me.bristermitten.mittenlib.codegen

import com.palantir.javapoet.ArrayTypeName
import com.palantir.javapoet.ClassName
import com.palantir.javapoet.MethodSpec
import java.util.Arrays
import java.util.Objects
import javax.lang.model.element.Modifier
import me.bristermitten.mittenlib.codegen.record.RecordConstructorSpec
import scala.jdk.CollectionConverters.*

import _root_.me.bristermitten.mittenlib.codegen.dsl.*
import _root_.me.bristermitten.mittenlib.codegen.dsl.BlockBuilder.*
import _root_.me.bristermitten.mittenlib.codegen.dsl.given

object BoilerplateGenerator:

  def genEquals(recordConstructorSpec: RecordConstructorSpec, name: ClassName): MethodSpec =
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
      val targetType = TypeRef.of(name)
      ifThen(!context.instanceOf(targetType)) {
        return_(Expr.bool(false))
      }
      val that = declare(targetType, "that", context.cast(targetType))

      val fields = recordConstructorSpec.fields.asScala.toList
      if (fields.isEmpty) {
        return_(Expr.bool(true))
      } else {
        var equalsExpr: Expr = equalsCall(fields.head, that)
        for (field <- fields.tail) {
          equalsExpr = Expr.BinaryOp(equalsExpr, "&&", equalsCall(field, that))
        }
        return_(equalsExpr)
      }
    }
    CodeBlockRenderer.renderMethod(methodDecl)

  private def equalsCall(fieldSpec: RecordConstructorSpec.RecordFieldSpec, that: Var): Expr =
    val name = fieldSpec.name
    val thisField = Expr.This.field(name)
    val thatField = that.field(name)
    fieldSpec.`type` match {
      case _: ArrayTypeName =>
        Expr.staticCall(TypeRef.of(classOf[Arrays]), "equals", thisField, thatField)
      case _ =>
        Expr.staticCall(TypeRef.of(classOf[Objects]), "equals", thisField, thatField)
    }

  private def hashCodeCall(fieldSpec: RecordConstructorSpec.RecordFieldSpec): Expr =
    val name = fieldSpec.name
    val thisField = Expr.This.field(name)
    fieldSpec.`type` match {
      case _: ArrayTypeName =>
        Expr.staticCall(TypeRef.of(classOf[Arrays]), "hashCode", thisField)
      case _ =>
        Expr.staticCall(TypeRef.of(classOf[Objects]), "hashCode", thisField)
    }

  private def toStringCall(fieldSpec: RecordConstructorSpec.RecordFieldSpec): Expr =
    val name = fieldSpec.name
    val thisField = Expr.This.field(name)
    fieldSpec.`type` match {
      case _: ArrayTypeName =>
        Expr.staticCall(TypeRef.of(classOf[Arrays]), "toString", thisField)
      case _ =>
        thisField
    }

  def genHashCode(recordConstructorSpec: RecordConstructorSpec): MethodSpec =
    val methodDecl = MethodDecl.build(
      name = "hashCode",
      returnType = Types.Int,
      parameters = Nil,
      modifiers = List(Modifier.PUBLIC)
    ) {
      val fields = recordConstructorSpec.fields.asScala.toList
      if (fields.isEmpty) {
        return_(Expr.staticCall(TypeRef.of(classOf[System]), "identityHashCode", Expr.This))
      } else {
        val args = fields.map(hashCodeCall)
        return_(Expr.staticCall(TypeRef.of(classOf[Objects]), "hash", args *))
      }
    }
    CodeBlockRenderer.renderMethod(methodDecl)

  def genToString(recordConstructorSpec: RecordConstructorSpec, name: ClassName): MethodSpec =
    val fields = recordConstructorSpec.fields.asScala.toList
    var expr: Expr = Expr.str(s"${name.simpleName()}{")

    for ((field, idx) <- fields.zipWithIndex) {
      expr = Expr.BinaryOp(expr, "+", Expr.str(s"${field.name}="))
      expr = Expr.BinaryOp(expr, "+", toStringCall(field))
      if (idx != fields.size - 1) {
        expr = Expr.BinaryOp(expr, "+", Expr.str(", "))
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

