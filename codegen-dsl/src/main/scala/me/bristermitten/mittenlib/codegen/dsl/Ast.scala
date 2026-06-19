package me.bristermitten.mittenlib.codegen.dsl

import com.palantir.javapoet.*

import javax.lang.model.element.{Modifier, TypeElement}
import scala.jdk.CollectionConverters.*

// ─── TypeRef ──────────────────────────────────────────────────────────────────

/** Reference to a Java type. Wraps JavaPoet's TypeName with a richer factory
  * API.
  */
enum TypeRef:
  case Simple(typeName: TypeName)
  case Parameterized(raw: ClassName, typeArgs: List[TypeRef])
  case ArrayOf(componentType: TypeRef)

  def toTypeName: TypeName = this match
    case Simple(t)            => t
    case Parameterized(r, as) =>
      ParameterizedTypeName.get(r, as.map(_.toTypeName)*)
    case ArrayOf(c) => ArrayTypeName.of(c.toTypeName)

  def apply(args: TypeRef*): TypeRef = this match
    case Simple(t: ClassName) => Parameterized(t, args.toList)
    case _ => throw IllegalArgumentException(s"Cannot parameterize $this")

  def array: TypeRef = ArrayOf(this)

object TypeRef:
  def of(cls: Class[?]): TypeRef = Simple(ClassName.get(cls))

  def of(name: TypeName): TypeRef = Simple(name)

  def of(name: ClassName): TypeRef = Simple(name)

  def of(name: TypeElement): TypeRef = Simple(ClassName.get(name))

  def of(pkg: String, n: String): TypeRef = Simple(ClassName.get(pkg, n))

// ─── Var ──────────────────────────────────────────────────────────────────────

/** A handle to a declared variable. Implements Expr so it's usable directly in
  * expression contexts: `myVar.call("toString")`, `ResultExpr.ok(myVar)`, etc.
  */
case class Var(generatedName: String, tpe: TypeRef) extends Expr:
  /** Use this Var as an expression — identity, but reads clearly in chains. */
  def ref: Expr = this

// ─── Expr ─────────────────────────────────────────────────────────────────────

/** Pure data representing a Java expression. All variants are case classes —
  * immutable, no side effects. Fluent composition methods are available on all
  * Exprs.
  *
  * Since [[Var]] extends [[Expr]], variables are first-class expressions.
  */
sealed trait Expr:
  def call(method: String, args: Expr*): Expr =
    Expr.MethodCall(this, method, args.toList)

  def field(name: String): Expr = Expr.FieldAccess(this, name)

  def cast(t: TypeRef): Expr = Expr.Cast(t, this)

  def ===(other: Expr): Expr = Expr.BinaryOp(this, "==", other)

  def !==(other: Expr): Expr = Expr.BinaryOp(this, "!=", other)

  def isNull: Expr = Expr.BinaryOp(this, "==", Expr.Null)

  def isNotNull: Expr = Expr.BinaryOp(this, "!=", Expr.Null)

  def <(other: Expr): Expr = Expr.BinaryOp(this, "<", other)

  def <=(other: Expr): Expr = Expr.BinaryOp(this, "<=", other)

  def >(other: Expr): Expr = Expr.BinaryOp(this, ">", other)

  def >=(other: Expr): Expr = Expr.BinaryOp(this, ">=", other)

  def &&(other: Expr): Expr = Expr.BinaryOp(this, "&&", other)

  def ||(other: Expr): Expr = Expr.BinaryOp(this, "||", other)

  def unary_! : Expr = Expr.UnaryOp("!", this)

  def instanceOf(t: TypeRef): Expr = Expr.InstanceOf(this, t)

object Expr:
  // Leaves
  case class Literal(rendered: String) extends Expr

  case object Null extends Expr

  case class BoolLit(value: Boolean) extends Expr

  case object This extends Expr

  case object Super extends Expr

  // Composite
  case class MethodCall(receiver: Expr, method: String, args: List[Expr])
      extends Expr

  case class StaticCall(tpe: TypeRef, method: String, args: List[Expr])
      extends Expr

  case class FieldAccess(receiver: Expr, fieldName: String) extends Expr

  case class StaticField(tpe: TypeRef, fieldName: String) extends Expr

  case class MethodRef(receiver: Expr, method: String) extends Expr

  case class StaticMethodRef(tpe: TypeRef, method: String) extends Expr

  case class NewInstance(tpe: TypeRef, args: List[Expr]) extends Expr

  case class NewAnonymousInstance(tpe: TypeRef, args: List[Expr]) extends Expr

  case class Cast(tpe: TypeRef, expr: Expr) extends Expr

  case class InstanceOf(expr: Expr, tpe: TypeRef) extends Expr

  case class Ternary(cond: Expr, ifTrue: Expr, ifFalse: Expr) extends Expr

  case class BinaryOp(left: Expr, op: String, right: Expr) extends Expr

  case class UnaryOp(op: String, operand: Expr) extends Expr

  // Lambdas
  case class Lambda(params: List[Var], body: Block[?]) extends Expr

  case class LambdaExpr(params: List[Var], body: Expr) extends Expr

  // Factories
  def str(s: String): Expr = Literal(s"\"$s\"")

  def int(n: Int): Expr = Literal(n.toString)

  def bool(b: Boolean): Expr = BoolLit(b)

  def staticCall(t: TypeRef, m: String, args: Expr*): Expr =
    StaticCall(t, m, args.toList)

  def new_(t: TypeRef, args: Expr*): Expr = NewInstance(t, args.toList)

  def newAnonymous(t: TypeRef, args: Expr*): Expr =
    NewAnonymousInstance(t, args.toList)

  def staticField(t: TypeRef, f: String): Expr = StaticField(t, f)

  def methodRef(receiver: Expr, method: String): Expr =
    MethodRef(receiver, method)

  def staticMethodRef(t: TypeRef, m: String): Expr = StaticMethodRef(t, m)

  def lambda(body: Block[?], params: Var*): Expr = Lambda(params.toList, body)

  def lambdaExpr(body: Expr, params: Var*): Expr =
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
  case class Return(value: Expr) extends Terminator

  case object ReturnVoid extends Terminator

  case class Throw(expr: Expr) extends Terminator

// ─── Statement ────────────────────────────────────────────────────────────────

sealed trait Statement

object Statement:
  case class DeclareAssign(variable: Var, value: Expr) extends Statement

  case class Assign(target: Expr, value: Expr) extends Statement

  case class ExprStatement(expr: Expr) extends Statement

  case class IfThen(cond: Expr, body: Block[?]) extends Statement

  case class IfThenElse(cond: Expr, thenBlock: Block[?], elseBlock: Block[?])
      extends Statement

  case class ForEach(element: Var, iterable: Expr, body: Block[?])
      extends Statement

  case class ForLoop(init: Statement, cond: Expr, update: Expr, body: Block[?])
      extends Statement

  case class TryCatch(
      tryBody: Block[?],
      exType: TypeRef,
      exVar: Var,
      catchBody: Block[?]
  ) extends Statement

  case object BlankLine extends Statement

  case class Comment(text: String) extends Statement

// ─── Declarations ────────────────────────────────────────────────────────────

case class FieldDecl(
    name: String,
    tpe: TypeRef,
    modifiers: List[Modifier] = Nil
)

case class MethodDecl(
    name: String,
    returnType: TypeRef,
    parameters: List[Var],
    modifiers: List[Modifier] = Nil,
    annotations: List[AnnotationSpec] = Nil,
    body: Block[?]
)

object MethodDecl:
  def build[S](
      name: String,
      returnType: TypeRef,
      parameters: List[Var],
      modifiers: List[Modifier] = Nil,
      annotations: List[AnnotationSpec] = Nil
  )(body: BlockBuilder ?=> Block[S]): MethodDecl =
    val block = BlockBuilder.build(body)
    MethodDecl(name, returnType, parameters, modifiers, annotations, block)

case class ClassDecl(
    packageName: String,
    name: String,
    modifiers: List[Modifier] = Nil,
    superinterfaces: List[TypeRef] = Nil,
    fields: List[FieldDecl] = Nil,
    methods: List[MethodDecl] = Nil
)
