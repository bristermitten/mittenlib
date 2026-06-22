package me.bristermitten.mittenlib.codegen.dsl

import com.palantir.javapoet.*

import javax.lang.model.element.{Modifier, TypeElement}
import scala.jdk.CollectionConverters.*

// ─── TypeRef ──────────────────────────────────────────────────────────────────

/** Reference to a Java type. Wraps JavaPoet's TypeName with a richer factory
  * API.
  */
enum TypeRef[+T]:
  case Simple(typeName: TypeName) extends TypeRef[Nothing]
  case Parameterized(raw: ClassName, typeArgs: List[TypeRef[?]])
      extends TypeRef[Nothing]
  case ArrayOf(componentType: TypeRef[?]) extends TypeRef[Nothing]

  def toTypeName: TypeName = this match
    case Simple(t)            => t
    case Parameterized(r, as) =>
      ParameterizedTypeName.get(r, as.map(_.toTypeName)*)
    case ArrayOf(c) => ArrayTypeName.of(c.toTypeName)

  def apply(args: TypeRef[?]*): TypeRef[Any] = this match
    case Simple(t: ClassName) => Parameterized(t, args.toList)
    case _ => throw IllegalArgumentException(s"Cannot parameterize $this")

  def array: TypeRef[Any] = ArrayOf(this)

object TypeRef:
  def of[T](cls: Class[T]): TypeRef[T] =
    Simple(ClassName.get(cls)).asInstanceOf[TypeRef[T]]

  def of(name: TypeName): TypeRef[Any] = Simple(name)

  def of(name: ClassName): TypeRef[Any] = Simple(name)

  def of(name: TypeElement): TypeRef[Any] = Simple(ClassName.get(name))

  def of(pkg: String, n: String): TypeRef[Any] = Simple(ClassName.get(pkg, n))

// ─── Var ──────────────────────────────────────────────────────────────────────

/** A handle to a declared variable. Implements Expr so it's usable directly in
  * expression contexts: `myVar.call("toString")`, `ResultExpr.ok(myVar)`, etc.
  */
case class Var[+T](generatedName: String, tpe: TypeRef[T]) extends Expr[T]:
  /** Use this Var as an expression — identity, but reads clearly in chains. */
  def ref: Expr[T] = this

// ─── Expr ─────────────────────────────────────────────────────────────────────

/** Pure data representing a Java expression. All variants are case classes —
  * immutable, no side effects. Fluent composition methods are available on all
  * Exprs.
  *
  * Since [[Var]] extends [[Expr]], variables are first-class expressions.
  */
sealed trait Expr[+T]:
  def call(method: String, args: Expr[?]*): Expr[Any] =
    Expr.MethodCall(this, method, args.toList)

  def field(name: String): Expr[Any] = Expr.FieldAccess(this, name)

  def cast(t: TypeRef[?]): Expr[Any] = Expr.Cast(t, this)

  def ===(other: Expr[?]): Expr[Boolean] = Expr.BinaryOp(this, "==", other)

  def !==(other: Expr[?]): Expr[Boolean] = Expr.BinaryOp(this, "!=", other)

  def isNull: Expr[Boolean] = Expr.BinaryOp(this, "==", Expr.Null)

  def isNotNull: Expr[Boolean] = Expr.BinaryOp(this, "!=", Expr.Null)

  def <(other: Expr[?]): Expr[Boolean] = Expr.BinaryOp(this, "<", other)

  def <=(other: Expr[?]): Expr[Boolean] = Expr.BinaryOp(this, "<=", other)

  def >(other: Expr[?]): Expr[Boolean] = Expr.BinaryOp(this, ">", other)

  def >=(other: Expr[?]): Expr[Boolean] = Expr.BinaryOp(this, ">=", other)

  def &&(other: Expr[?]): Expr[Boolean] = Expr.BinaryOp(this, "&&", other)

  def ||(other: Expr[?]): Expr[Boolean] = Expr.BinaryOp(this, "||", other)

  def unary_! : Expr[Boolean] = Expr.UnaryOp("!", this)

  def instanceOf(t: TypeRef[?]): Expr[Boolean] = Expr.InstanceOf(this, t)

object Expr:
  // Leaves
  case class Literal(rendered: String) extends Expr[Nothing]

  case object Null extends Expr[Nothing]

  case class BoolLit(value: Boolean) extends Expr[Boolean]

  case object This extends Expr[Nothing]

  case object Super extends Expr[Nothing]

  // Composite
  case class MethodCall(receiver: Expr[?], method: String, args: List[Expr[?]])
      extends Expr[Any]

  case class StaticCall(tpe: TypeRef[?], method: String, args: List[Expr[?]])
      extends Expr[Any]

  case class FieldAccess(receiver: Expr[?], fieldName: String) extends Expr[Any]

  case class StaticField(tpe: TypeRef[?], fieldName: String) extends Expr[Any]

  case class MethodRef(receiver: Expr[?], method: String) extends Expr[Any]

  case class StaticMethodRef(tpe: TypeRef[?], method: String) extends Expr[Any]

  case class NewInstance(tpe: TypeRef[?], args: List[Expr[?]]) extends Expr[Any]

  case class NewAnonymousInstance(tpe: TypeRef[?], args: List[Expr[?]])
      extends Expr[Any]

  case class Cast(tpe: TypeRef[?], expr: Expr[?]) extends Expr[Any]

  case class InstanceOf(expr: Expr[?], tpe: TypeRef[?]) extends Expr[Boolean]

  case class Ternary(cond: Expr[?], ifTrue: Expr[?], ifFalse: Expr[?])
      extends Expr[Any]

  case class BinaryOp(left: Expr[?], op: String, right: Expr[?])
      extends Expr[Boolean]

  case class UnaryOp(op: String, operand: Expr[?]) extends Expr[Boolean]

  // Lambdas
  case class Lambda(params: List[Var[?]], body: Block[?]) extends Expr[Any]

  case class LambdaExpr(params: List[Var[?]], body: Expr[?]) extends Expr[Any]

  // Factories
  def str(s: String): Expr[String] =
    Literal(s"\"$s\"").asInstanceOf[Expr[String]]

  def int(n: Int): Expr[Int] = Literal(n.toString).asInstanceOf[Expr[Int]]

  def bool(b: Boolean): Expr[Boolean] = BoolLit(b)

  def staticCall(t: TypeRef[?], m: String, args: Expr[?]*): Expr[Any] =
    StaticCall(t, m, args.toList)

  def new_(t: TypeRef[?], args: Expr[?]*): Expr[Any] =
    NewInstance(t, args.toList)

  def newAnonymous(t: TypeRef[?], args: Expr[?]*): Expr[Any] =
    NewAnonymousInstance(t, args.toList)

  def staticField(t: TypeRef[?], f: String): Expr[Any] = StaticField(t, f)

  def methodRef(receiver: Expr[?], method: String): Expr[Any] =
    MethodRef(receiver, method)

  def staticMethodRef(t: TypeRef[?], m: String): Expr[Any] =
    StaticMethodRef(t, m)

  def lambda(body: Block[?], params: Var[?]*): Expr[Any] =
    Lambda(params.toList, body)

  def lambdaExpr(body: Expr[?], params: Var[?]*): Expr[Any] =
    LambdaExpr(params.toList, body)

// ─── Block & Terminator ───────────────────────────────────────────────────────

/** Evidence of how a block ends, carried as a phantom type parameter.
  *
  *   - [[Block[Terminated]]] — has an explicit return/throw. Method bodies
  *     require this.
  *   - [[Block[Open]]] — no explicit terminator (void bodies, loop bodies,
  *     etc.)
  *
  * The phantom type means `ifThenElse` taking two `Builder ?=>
  * Block[Terminated]` arguments returns `Block[Terminated]` — no Optional
  * needed.
  */
sealed trait Terminated

sealed trait Open

case class Block[+S](
    statements: List[Statement],
    terminator: Option[Terminator]
):
  def isTerminated: Boolean = terminator.isDefined

object Block:
  def empty: Block[Open] = Block(Nil, None)

sealed trait Terminator

object Terminator:
  case class Return(value: Expr[?]) extends Terminator

  case object ReturnVoid extends Terminator

  case class Throw(expr: Expr[?]) extends Terminator

// ─── Statement ────────────────────────────────────────────────────────────────

sealed trait Statement

object Statement:
  case class DeclareAssign(variable: Var[?], value: Expr[?]) extends Statement

  case class Assign(target: Expr[?], value: Expr[?]) extends Statement

  case class ExprStatement(expr: Expr[?]) extends Statement

  case class IfThen(cond: Expr[?], body: Block[?]) extends Statement

  case class IfThenElse(cond: Expr[?], thenBlock: Block[?], elseBlock: Block[?])
      extends Statement

  case class ForEach(element: Var[?], iterable: Expr[?], body: Block[?])
      extends Statement

  case class ForLoop(
      init: Statement,
      cond: Expr[?],
      update: Expr[?],
      body: Block[?]
  ) extends Statement

  case class TryCatch(
      tryBody: Block[?],
      exType: TypeRef[?],
      exVar: Var[?],
      catchBody: Block[?]
  ) extends Statement

  case object BlankLine extends Statement

  case class Comment(text: String) extends Statement

// ─── Declarations ────────────────────────────────────────────────────────────

case class FieldDecl(
    name: String,
    tpe: TypeRef[?],
    modifiers: List[Modifier] = Nil,
    initializer: Option[Expr[?]] = None,
    annotations: List[AnnotationSpec] = Nil,
    javadoc: Option[String] = None
)

case class ConstructorDecl(
    parameters: List[Var[?]],
    modifiers: List[Modifier] = Nil,
    annotations: List[AnnotationSpec] = Nil,
    body: Block[?]
)

object ConstructorDecl:
  def build[S](
      parameters: List[Var[?]],
      modifiers: List[Modifier] = Nil,
      annotations: List[AnnotationSpec] = Nil
  )(body: BlockBuilder ?=> Block[S]): ConstructorDecl =
    val block = BlockBuilder.build(body)
    ConstructorDecl(parameters, modifiers, annotations, block)

case class MethodDecl(
    name: String,
    returnType: TypeRef[?],
    parameters: List[Var[?]],
    modifiers: List[Modifier] = Nil,
    annotations: List[AnnotationSpec] = Nil,
    body: Block[?]
)

object MethodDecl:
  def build[S](
      name: String,
      returnType: TypeRef[?],
      parameters: List[Var[?]],
      modifiers: List[Modifier] = Nil,
      annotations: List[AnnotationSpec] = Nil
  )(body: BlockBuilder ?=> Block[S]): MethodDecl =
    val block = BlockBuilder.build(body)
    MethodDecl(name, returnType, parameters, modifiers, annotations, block)

case class ClassDecl(
    packageName: String,
    name: String,
    modifiers: List[Modifier] = Nil,
    annotations: List[AnnotationSpec] = Nil,
    superclass: Option[TypeRef[?]] = None,
    superinterfaces: List[TypeRef[?]] = Nil,
    constructors: List[ConstructorDecl] = Nil,
    fields: List[FieldDecl] = Nil,
    methods: List[MethodDecl] = Nil,
    nestedTypes: List[ClassDecl] = Nil,
    extraMethods: List[MethodSpec] = Nil
)
