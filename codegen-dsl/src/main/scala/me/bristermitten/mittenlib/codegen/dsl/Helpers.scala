package me.bristermitten.mittenlib.codegen.dsl

import com.palantir.javapoet.{ClassName, TypeName}

/** Centralized [[TypeRef]] constants for commonly used types. */
object Types:
  // Java primitives
  val Int     : TypeRef = TypeRef.of(TypeName.INT)
  val Long    : TypeRef = TypeRef.of(TypeName.LONG)
  val Double  : TypeRef = TypeRef.of(TypeName.DOUBLE)
  val Boolean : TypeRef = TypeRef.of(TypeName.BOOLEAN)
  val Void    : TypeRef = TypeRef.of(TypeName.VOID)

  // Java stdlib
  val String    : TypeRef = TypeRef.of(classOf[java.lang.String])
  val Object    : TypeRef = TypeRef.of(classOf[java.lang.Object])
  val Exception : TypeRef = TypeRef.of(classOf[java.lang.Exception])

  val List: TypeRef = TypeRef.of(classOf[java.util.List[?]])
  val Set : TypeRef = TypeRef.of(classOf[java.util.Set[?]])
  val Map : TypeRef = TypeRef.of(classOf[java.util.Map[?, ?]])

  def listOf(element: TypeRef): TypeRef = List(element)
  def setOf(element: TypeRef) : TypeRef = Set(element)
  def mapOf(k: TypeRef, v: TypeRef): TypeRef = Map(k, v)

  // MittenLib domain types
  val Result: TypeRef = TypeRef.of("me.bristermitten.mittenlib.util", "Result")
  val DataTree: TypeRef =
    TypeRef.of("me.bristermitten.mittenlib.config.tree", "DataTree")
  val DataTreeTransforms: TypeRef =
    TypeRef.of("me.bristermitten.mittenlib.config.tree", "DataTreeTransforms")
  val DeserializationContext: TypeRef =
    TypeRef.of("me.bristermitten.mittenlib.config", "DeserializationContext")
  val SerializationContext: TypeRef =
    TypeRef.of("me.bristermitten.mittenlib.config", "SerializationContext")
  val CollectionsUtils: TypeRef =
    TypeRef.of("me.bristermitten.mittenlib.util", "CollectionsUtils")
  val TypeToken: TypeRef =
    TypeRef.of("com.google.gson.reflect", "TypeToken")
  val Function: TypeRef =
    TypeRef.of(classOf[java.util.function.Function[?, ?]])
  val Provider: TypeRef =
    TypeRef.of(classOf[com.google.inject.Provider[?]])

  def resultOf(v: TypeRef): TypeRef = Result(v)

// ─── Typed expression helpers ─────────────────────────────────────────────────

/** Typed helpers for `Result<T>` expressions. */
object ResultExpr:
  def ok(value: Expr): Expr            = Expr.staticCall(Types.Result, "ok", value)
  def okNull: Expr                     = ok(Expr.Null)
  def fail(exception: Expr): Expr      = Expr.staticCall(Types.Result, "fail", exception)
  def getOrThrow(result: Expr): Expr   = result.call("getOrThrow")
  def map(result: Expr, f: Expr): Expr = result.call("map", f)
  def flatMap(result: Expr, f: Expr): Expr = result.call("flatMap", f)
  def isOk(result: Expr): Expr         = result.call("isOk")
  def isError(result: Expr): Expr      = result.call("isError")

/** Typed helpers for `DataTree` / `DataTreeTransforms` expressions. */
object DataTreeExpr:
  def loadFrom(data: Expr): Expr       = Expr.staticCall(Types.DataTreeTransforms, "loadFrom", data)
  def getData(ctx: Expr): Expr         = ctx.call("getData")
  def getString(dt: Expr): Expr        = dt.call("getString")
  def getInt(dt: Expr): Expr           = dt.call("getInt")
  def getLong(dt: Expr): Expr          = dt.call("getLong")
  def getDouble(dt: Expr): Expr        = dt.call("getDouble")
  def getBoolean(dt: Expr): Expr       = dt.call("getBoolean")
  def get(dt: Expr, key: Expr): Expr   = dt.call("get", key)
  def isNull(dt: Expr): Expr           = dt.call("isNull")
  def isList(dt: Expr): Expr           = dt.call("isList")
  def isMap(dt: Expr): Expr            = dt.call("isMap")
  def asList(dt: Expr): Expr           = dt.call("asList")
  def asMap(dt: Expr): Expr            = dt.call("asMap")

// ─── Staged Expression Wrapper ──────────────────────────────────────────────

import scala.language.implicitConversions
import scala.reflect.ClassTag
import _root_.me.bristermitten.mittenlib.config.DeserializationContext
import _root_.me.bristermitten.mittenlib.config.SerializationContext
import _root_.me.bristermitten.mittenlib.config.reader.ObjectMapper
import _root_.me.bristermitten.mittenlib.config.tree.DataTree
import _root_.me.bristermitten.mittenlib.util.Result

case class StagedExpr[+T](expr: Expr):
  // Compile-time phantom cast
  def as[U]: StagedExpr[U] = StagedExpr(expr)

  def asVar: Var = expr match
    case v: Var => v
    case _ => throw new IllegalArgumentException(s"StagedExpr is not a variable reference: $expr")

  // Operators
  def cast(t: TypeRef): StagedExpr[Any] = StagedExpr(expr.cast(t))
  def cast[U](using tag: ClassTag[U]): StagedExpr[U] = StagedExpr(expr.cast(TypeRef.of(tag.runtimeClass)))

  def ===(other: Expr): StagedExpr[Boolean] = StagedExpr(expr === other)
  def !==(other: Expr): StagedExpr[Boolean] = StagedExpr(expr !== other)
  def isNull: StagedExpr[Boolean] = StagedExpr(expr.isNull)
  def isNotNull: StagedExpr[Boolean] = StagedExpr(expr.isNotNull)
  def &&(other: Expr): StagedExpr[Boolean] = StagedExpr(expr && other)
  def ||(other: Expr): StagedExpr[Boolean] = StagedExpr(expr || other)
  def unary_! : StagedExpr[Boolean] = StagedExpr(!expr)
  def instanceOf(t: TypeRef): StagedExpr[Boolean] = StagedExpr(expr.instanceOf(t))
  def instanceOf[U](using tag: ClassTag[U]): StagedExpr[Boolean] = StagedExpr(expr.instanceOf(TypeRef.of(tag.runtimeClass)))

object StagedExpr:
  def param[T](tpe: TypeRef, name: String): StagedExpr[T] = StagedExpr(Var(name, tpe))
  def field[T](tpe: TypeRef, name: String): StagedExpr[T] = StagedExpr(Expr.FieldAccess(Expr.This, name))

given Conversion[StagedExpr[?], Expr] with
  def apply(se: StagedExpr[?]): Expr = se.expr

extension (e: Expr)
  def unary_~ : StagedExpr[Any] = StagedExpr(e)
  def ~ : StagedExpr[Any] = StagedExpr(e)

// ─── Staged Type-Safe Extension Methods ──────────────────────────────────────

import scala.annotation.targetName

extension (e: StagedExpr[DeserializationContext])
  def getData: StagedExpr[DataTree] = StagedExpr(e.expr.call("getData"))
  def withData(data: Expr): StagedExpr[DeserializationContext] = StagedExpr(e.expr.call("withData", data))
  def getMapper: StagedExpr[ObjectMapper] = StagedExpr(e.expr.call("getMapper"))

extension (e: StagedExpr[SerializationContext])
  @targetName("getMapperSerialization")
  def getMapper: StagedExpr[ObjectMapper] = StagedExpr(e.expr.call("getMapper"))

extension (e: StagedExpr[ObjectMapper])
  def map[U](data: Expr, typeToken: Expr): StagedExpr[Result[U]] = StagedExpr(e.expr.call("map", data, typeToken))
  def map[U](value: Expr): StagedExpr[Result[U]] = StagedExpr(e.expr.call("map", value))

extension (e: StagedExpr[DataTree])
  def value: StagedExpr[Any] = StagedExpr(e.expr.call("value"))
  def get(key: Expr): StagedExpr[DataTree] = StagedExpr(e.expr.call("get", key))
  def get(key: String): StagedExpr[DataTree] = StagedExpr(e.expr.call("get", Expr.str(key)))

extension [T](e: StagedExpr[com.google.inject.Provider[T]])
  def get: StagedExpr[T] = StagedExpr(e.expr.call("get"))

extension [A, B](e: StagedExpr[java.util.function.Function[A, B]])
  def apply(arg: Expr): StagedExpr[B] = StagedExpr(e.expr.call("apply", arg))

extension [T](e: StagedExpr[Result[T]])
  def getOrThrow: StagedExpr[T] = StagedExpr(e.expr.call("getOrThrow"))
  def mapResult[U](f: Expr): StagedExpr[Result[U]] = StagedExpr(e.expr.call("map", f))
  def flatMapResult[U](f: Expr): StagedExpr[Result[U]] = StagedExpr(e.expr.call("flatMap", f))
