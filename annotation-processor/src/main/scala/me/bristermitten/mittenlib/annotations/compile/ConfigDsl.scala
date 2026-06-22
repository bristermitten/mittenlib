package me.bristermitten.mittenlib.annotations.compile

import me.bristermitten.mittenlib.codegen.dsl.*
import me.bristermitten.mittenlib.config.reader.ObjectMapper
import me.bristermitten.mittenlib.config.tree.DataTree
import me.bristermitten.mittenlib.config.{
  DeserializationContext,
  SerializationContext
}
import me.bristermitten.mittenlib.util.Result
import com.palantir.javapoet.ClassName
import scala.annotation.targetName

object Types:
  val ConfigValidationException: TypeRef[Any] =
    TypeRef.of(
      "me.bristermitten.mittenlib.config.exception",
      "ConfigValidationException"
    )
  val Violation: TypeRef[Any] =
    TypeRef.of(
      "me.bristermitten.mittenlib.config.exception.ConfigValidationException",
      "Violation"
    )

  val Result: TypeRef[Result[?]] = TypeRef.of(classOf[Result[?]])
  val DataTree: TypeRef[DataTree] = TypeRef.of(classOf[DataTree])
  val DataTreeTransforms
      : TypeRef[me.bristermitten.mittenlib.config.tree.DataTreeTransforms] =
    TypeRef.of(
      classOf[me.bristermitten.mittenlib.config.tree.DataTreeTransforms]
    )

  val DeserializationContext: TypeRef[DeserializationContext] =
    TypeRef.of(classOf[DeserializationContext])
  val SerializationContext: TypeRef[SerializationContext] =
    TypeRef.of(classOf[SerializationContext])
  val CollectionsUtils
      : TypeRef[me.bristermitten.mittenlib.config.CollectionsUtils] =
    TypeRef.of(classOf[me.bristermitten.mittenlib.config.CollectionsUtils])
  val TypeToken: TypeRef[Any] =
    TypeRef.of("com.google.gson.reflect", "TypeToken")
  val ObjectMapper: TypeRef[ObjectMapper] =
    TypeRef.of(classOf[ObjectMapper])

  def resultOf[T](v: TypeRef[T]): TypeRef[Result[T]] =
    TypeRef.Parameterized(ClassName.get(classOf[Result[?]]), scala.List(v))

extension (e: Expr[DeserializationContext])
  @targetName("getMapperDeser")
  def getMapper: Expr[ObjectMapper] =
    Expr.MethodCall(e, "getMapper", Nil).asInstanceOf[Expr[ObjectMapper]]

  def getData: Expr[DataTree] =
    Expr.MethodCall(e, "getData", Nil).asInstanceOf[Expr[DataTree]]

  def withData(data: Expr[?]): Expr[DeserializationContext] =
    Expr
      .MethodCall(e, "withData", List(data))
      .asInstanceOf[Expr[DeserializationContext]]

extension (e: Expr[SerializationContext])
  @targetName("getMapperSer")
  def getMapper: Expr[ObjectMapper] =
    Expr.MethodCall(e, "getMapper", Nil).asInstanceOf[Expr[ObjectMapper]]

extension (e: Expr[ObjectMapper])
  def map[T](data: Expr[Any], typeToken: Expr[?]): Expr[Result[T]] =
    Expr
      .MethodCall(e, "map", List(data, typeToken))
      .asInstanceOf[Expr[Result[T]]]

  def map(value: Expr[Any]): Expr[Any] =
    Expr.MethodCall(e, "map", List(value))

extension (tpe: TypeRef[DataTree])
  def null_ : Expr[DataTree] =
    Expr.StaticCall(tpe, "null_", Nil).asInstanceOf[Expr[DataTree]]

extension (
    tpe: TypeRef[me.bristermitten.mittenlib.config.tree.DataTreeTransforms]
)
  def loadFrom(data: Expr[Any]): Expr[DataTree] =
    Expr.StaticCall(tpe, "loadFrom", List(data)).asInstanceOf[Expr[DataTree]]

extension (tpe: TypeRef[Result[?]])
  def ok[T](value: Expr[T]): Expr[Result[T]] =
    Expr.StaticCall(tpe, "ok", List(value)).asInstanceOf[Expr[Result[T]]]
  def fail[T](exception: Expr[Any]): Expr[Result[T]] =
    Expr.StaticCall(tpe, "fail", List(exception)).asInstanceOf[Expr[Result[T]]]

extension [T](e: Expr[Result[T]])
  def mapResult[U](f: Expr[?]): Expr[Result[U]] =
    Expr.MethodCall(e, "map", List(f)).asInstanceOf[Expr[Result[U]]]

  def getOrThrow: Expr[T] =
    Expr.MethodCall(e, "getOrThrow", Nil).asInstanceOf[Expr[T]]

extension (tpe: TypeRef[me.bristermitten.mittenlib.config.CollectionsUtils])
  def serializeList[T](
      input: Expr[java.util.List[T]],
      ctx: Expr[SerializationContext],
      mapper: Expr[Any]
  ): Expr[DataTree] =
    Expr
      .StaticCall(tpe, "serializeList", List(input, ctx, mapper))
      .asInstanceOf[Expr[DataTree]]

  def serializeSet[T](
      input: Expr[java.util.Set[T]],
      ctx: Expr[SerializationContext],
      mapper: Expr[Any]
  ): Expr[DataTree] =
    Expr
      .StaticCall(tpe, "serializeSet", List(input, ctx, mapper))
      .asInstanceOf[Expr[DataTree]]

  def serializeMap[K, V](
      input: Expr[java.util.Map[K, V]],
      ctx: Expr[SerializationContext],
      mapper: Expr[Any]
  ): Expr[DataTree] =
    Expr
      .StaticCall(tpe, "serializeMap", List(input, ctx, mapper))
      .asInstanceOf[Expr[DataTree]]

  def serializeOptional[T](
      input: Expr[java.util.Optional[T]],
      ctx: Expr[SerializationContext],
      mapper: Expr[Any]
  ): Expr[DataTree] =
    Expr
      .StaticCall(tpe, "serializeOptional", List(input, ctx, mapper))
      .asInstanceOf[Expr[DataTree]]

object ResultExpr:
  def ok[T](value: Expr[T]): Expr[Result[T]] =
    Types.Result.ok(value)

  def fail[T](exception: Expr[Any]): Expr[Result[T]] =
    Types.Result.fail(exception)

  def okNull: Expr[Result[Any]] =
    Types.Result.ok(Expr.Null)

object DataTreeExpr:
  def loadFrom(data: Expr[Any]): Expr[DataTree] =
    Types.DataTreeTransforms.loadFrom(data)
