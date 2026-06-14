package me.bristermitten.mittenlib.codegen.dsl

import com.palantir.javapoet.{ArrayTypeName, ClassName, ParameterizedTypeName, TypeName}
import java.util.concurrent.ConcurrentHashMap
import javax.lang.model.element.TypeElement
import scala.jdk.CollectionConverters.*

// ─── TypeRef ──────────────────────────────────────────────────────────────────

/** Reference to a Java type. Wraps JavaPoet's TypeName with a richer factory API. */
enum TypeRef:
  case Simple(typeName: TypeName)
  case Parameterized(raw: ClassName, typeArgs: List[TypeRef])
  case ArrayOf(componentType: TypeRef)

  def toTypeName: TypeName = this match
    case Simple(t) => t
    case Parameterized(r, as) => ParameterizedTypeName.get(r, as.map(_.toTypeName) *)
    case ArrayOf(c) => ArrayTypeName.of(c.toTypeName)

  def apply(args: TypeRef*): TypeRef = this match
    case Simple(t: ClassName) => Parameterized(t, args.toList)
    case _ => throw IllegalArgumentException(s"Cannot parameterize $this")

  def array: TypeRef = ArrayOf(this)

  def toJavaClass: Option[Class[?]] = AstValidator.toJavaClass(this)

object TypeRef:
  def of(cls: Class[?]): TypeRef = Simple(ClassName.get(cls))

  def of(name: TypeName): TypeRef = Simple(name)

  def of(name: ClassName): TypeRef = Simple(name)

  def of(name: TypeElement): TypeRef = Simple(ClassName.get(name))

  def of(pkg: String, n: String): TypeRef = Simple(ClassName.get(pkg, n))

// ─── Var ──────────────────────────────────────────────────────────────────────

/**
 * A handle to a declared variable. Implements Expr so it's usable directly in
 * expression contexts: `myVar.call("toString")`, `ResultExpr.ok(myVar)`, etc.
 */
case class Var(generatedName: String, tpe: TypeRef) extends Expr:
  /** Use this Var as an expression — identity, but reads clearly in chains. */
  def ref: Expr = this

// ─── Expr ─────────────────────────────────────────────────────────────────────

/**
 * Pure data representing a Java expression. All variants are case classes —
 * immutable, no side effects. Fluent composition methods are available on all Exprs.
 *
 * Since [[Var]] extends [[Expr]], variables are first-class expressions.
 */
sealed trait Expr:
  def call(method: String, args: Expr*): Expr = Expr.MethodCall(this, method, args.toList)

  def field(name: String): Expr = Expr.FieldAccess(this, name)

  def cast(t: TypeRef): Expr = Expr.Cast(t, this)

  def ===(other: Expr): Expr = Expr.BinaryOp(this, "==", other)

  def !==(other: Expr): Expr = Expr.BinaryOp(this, "!=", other)

  def isNull: Expr = Expr.BinaryOp(this, "==", Expr.Null)

  def isNotNull: Expr = Expr.BinaryOp(this, "!=", Expr.Null)

  def &&(other: Expr): Expr = Expr.BinaryOp(this, "&&", other)

  def ||(other: Expr): Expr = Expr.BinaryOp(this, "||", other)

  def unary_! : Expr = Expr.UnaryOp("!", this)

  def instanceOf(t: TypeRef): Expr = Expr.InstanceOf(this, t)

  def exprType: Option[TypeRef] = this match
    case Var(_, t) => Some(t)
    case Expr.Literal(s) =>
      if (s.startsWith("\"") && s.endsWith("\"")) Some(TypeRef.of(classOf[String]))
      else None
    case Expr.Null => None
    case Expr.BoolLit(_) => Some(TypeRef.of(TypeName.BOOLEAN))
    case Expr.This => None
    case Expr.Super => None
    case Expr.Cast(t, _) => Some(t)
    case Expr.InstanceOf(_, _) => Some(TypeRef.of(TypeName.BOOLEAN))
    case Expr.NewInstance(t, _) => Some(t)
    case Expr.NewAnonymousInstance(t, _) => Some(t)
    case Expr.Ternary(_, ifTrue, _) => ifTrue.exprType
    case Expr.BinaryOp(_, op, _) =>
      if (Seq("==", "!=", "<", ">", "<=", ">=", "&&", "||").contains(op)) Some(TypeRef.of(TypeName.BOOLEAN))
      else None
    case Expr.UnaryOp(op, operand) =>
      if (op == "!") Some(TypeRef.of(TypeName.BOOLEAN))
      else operand.exprType
    case Expr.MethodCall(receiver, method, _) =>
      AstValidator.findMethodReturnType(receiver.exprType, method)
    case Expr.StaticCall(t, method, _) =>
      AstValidator.findMethodReturnType(Some(t), method)
    case Expr.FieldAccess(receiver, fieldName) =>
      AstValidator.findFieldType(receiver.exprType, fieldName)
    case Expr.StaticField(t, fieldName) =>
      AstValidator.findFieldType(Some(t), fieldName)
    case Expr.MethodRef(_, _) => None
    case Expr.StaticMethodRef(_, _) => None
    case Expr.Lambda(_, _) => None
    case Expr.LambdaExpr(_, _) => None

object Expr:
  // Leaves
  case class Literal(rendered: String) extends Expr

  case object Null extends Expr

  case class BoolLit(value: Boolean) extends Expr

  case object This extends Expr

  case object Super extends Expr

  // Composite
  case class MethodCall(receiver: Expr, method: String, args: List[Expr]) extends Expr

  case class StaticCall(tpe: TypeRef, method: String, args: List[Expr]) extends Expr

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

  def staticCall(t: TypeRef, m: String, args: Expr*): Expr = StaticCall(t, m, args.toList)

  def new_(t: TypeRef, args: Expr*): Expr = NewInstance(t, args.toList)

  def newAnonymous(t: TypeRef, args: Expr*): Expr = NewAnonymousInstance(t, args.toList)

  def staticField(t: TypeRef, f: String): Expr = StaticField(t, f)

  def methodRef(receiver: Expr, method: String): Expr = MethodRef(receiver, method)

  def staticMethodRef(t: TypeRef, m: String): Expr = StaticMethodRef(t, m)

  def lambda(body: Block[?], params: Var*): Expr = Lambda(params.toList, body)

  def lambdaExpr(body: Expr, params: Var*): Expr = LambdaExpr(params.toList, body)

// ─── Block & Terminator ───────────────────────────────────────────────────────

/**
 * Evidence of how a block ends, carried as a phantom type parameter.
 *
 * - [[Block[Terminated]]] — has an explicit return/throw. Method bodies require this.
 * - [[Block[Open]]]       — no explicit terminator (void bodies, loop bodies, etc.)
 *
 * The phantom type means `ifThenElse` taking two `Builder ?=> Block[Terminated]`
 * arguments returns `Block[Terminated]` — no Optional needed.
 */
sealed trait Terminated

sealed trait Open

case class Block[+S](statements: List[Statement], terminator: Option[Terminator]):
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

  case class IfThenElse(cond: Expr, thenBlock: Block[?], elseBlock: Block[?]) extends Statement

  case class ForEach(element: Var, iterable: Expr, body: Block[?]) extends Statement

  case class ForLoop(init: Statement, cond: Expr, update: Expr, body: Block[?]) extends Statement

  case class TryCatch(tryBody: Block[?], exType: TypeRef, exVar: Var,
                      catchBody: Block[?]) extends Statement

  case object BlankLine extends Statement

  case class Comment(text: String) extends Statement

// ─── Validation ──────────────────────────────────────────────────────────────

class AstValidationException(message: String) extends IllegalArgumentException(message)

object AstValidator:
  private val classCache = new ConcurrentHashMap[String, Option[Class[?]]]()
  private val methodCache = new ConcurrentHashMap[Class[?], List[java.lang.reflect.Method]]()
  private val fieldCache = new ConcurrentHashMap[Class[?], List[java.lang.reflect.Field]]()

  private def loadClass(name: String): Option[Class[?]] =
    val cached = classCache.get(name)
    if (cached != null) cached
    else
      val cl = Thread.currentThread().getContextClassLoader
      val res = try Some(Class.forName(name, false, cl))
      catch
        case _: ClassNotFoundException =>
          try Some(Class.forName(name, false, getClass.getClassLoader))
          catch case _: ClassNotFoundException => None
      classCache.put(name, res)
      res

  def toJavaClass(tpe: TypeRef): Option[Class[?]] = tpe match
    case TypeRef.Simple(t: ClassName) =>
      loadClass(t.reflectionName())
    case TypeRef.Simple(t) =>
      if (t == TypeName.INT) Some(java.lang.Integer.TYPE)
      else if (t == TypeName.LONG) Some(java.lang.Long.TYPE)
      else if (t == TypeName.DOUBLE) Some(java.lang.Double.TYPE)
      else if (t == TypeName.FLOAT) Some(java.lang.Float.TYPE)
      else if (t == TypeName.SHORT) Some(java.lang.Short.TYPE)
      else if (t == TypeName.BYTE) Some(java.lang.Byte.TYPE)
      else if (t == TypeName.CHAR) Some(java.lang.Character.TYPE)
      else if (t == TypeName.BOOLEAN) Some(java.lang.Boolean.TYPE)
      else if (t == TypeName.VOID) Some(java.lang.Void.TYPE)
      else if (t.isInstanceOf[ClassName]) loadClass(t.asInstanceOf[ClassName].reflectionName())
      else loadClass(t.toString)
    case TypeRef.Parameterized(raw, _) =>
      loadClass(raw.reflectionName())
    case TypeRef.ArrayOf(componentType) =>
      toJavaClass(componentType).flatMap { componentClass =>
        try Some(java.lang.reflect.Array.newInstance(componentClass, 0).getClass)
        catch case _: Exception => None
      }

  private def getAllMethods(clazz: Class[?]): List[java.lang.reflect.Method] =
    if (clazz == null) Nil
    else
      val cached = methodCache.get(clazz)
      if (cached != null) cached
      else
        val declared = try clazz.getDeclaredMethods.toList catch case _: SecurityException | _: NoClassDefFoundError => Nil
        val inherited = getAllMethods(clazz.getSuperclass) ++ clazz.getInterfaces.flatMap(getAllMethods)
        val res = (declared ++ inherited).distinct
        methodCache.put(clazz, res)
        res

  private def formatAvailableMethods(clazz: Class[?]): String =
    val methods = getAllMethods(clazz)
      .map { m =>
        val params = m.getParameterTypes.map(_.getSimpleName).mkString(", ")
        s"  - ${m.getName}($params)"
      }
      .sorted
      .distinct
      .mkString("\n")
    if (methods.isEmpty) "  (none)" else methods

  def findMethodReturnType(receiverTpe: Option[TypeRef], methodName: String): Option[TypeRef] =
    receiverTpe.flatMap(toJavaClass).flatMap { clazz =>
      val methods = getAllMethods(clazz).filter(_.getName == methodName)
      if (methods.nonEmpty) {
        val retClass = methods.head.getReturnType
        Some(TypeRef.of(retClass))
      } else {
        None
      }
    }

  private def getAllFields(clazz: Class[?]): List[java.lang.reflect.Field] =
    if (clazz == null) Nil
    else
      val cached = fieldCache.get(clazz)
      if (cached != null) cached
      else
        val declared = try clazz.getDeclaredFields.toList catch case _: SecurityException | _: NoClassDefFoundError => Nil
        val inherited = getAllFields(clazz.getSuperclass)
        val res = (declared ++ inherited).distinct
        fieldCache.put(clazz, res)
        res

  private def formatAvailableFields(clazz: Class[?]): String =
    val fields = getAllFields(clazz)
      .map { f => s"  - ${f.getName}: ${f.getType.getSimpleName}" }
      .sorted
      .distinct
      .mkString("\n")
    if (fields.isEmpty) "  (none)" else fields

  def findFieldType(receiverTpe: Option[TypeRef], fieldName: String): Option[TypeRef] =
    if (fieldName == "class") Some(TypeRef.of(classOf[Class[?]]))
    else
      receiverTpe.flatMap(toJavaClass).flatMap { clazz =>
        if (clazz.isArray && fieldName == "length") Some(TypeRef.of(TypeName.INT))
        else
          getAllFields(clazz).find(_.getName == fieldName).map { field =>
            TypeRef.of(field.getType)
          }
      }

  def validateMethodCall(receiver: Expr, method: String): Unit =
    receiver.exprType.foreach { rTpe =>
      toJavaClass(rTpe).foreach { clazz =>
        val hasMethod = getAllMethods(clazz).exists(_.getName == method)
        if (!hasMethod) {
          throw AstValidationException(
            s"Method '$method' not found on class '${clazz.getName}'.\nAvailable methods:\n${formatAvailableMethods(clazz)}"
          )
        }
      }
    }

  def validateStaticCall(tpe: TypeRef, method: String): Unit =
    toJavaClass(tpe).foreach { clazz =>
      val hasMethod = getAllMethods(clazz).exists(_.getName == method)
      if (!hasMethod) {
        throw AstValidationException(
          s"Static method '$method' not found on class '${clazz.getName}'.\nAvailable methods:\n${formatAvailableMethods(clazz)}"
        )
      }
    }

  def validateFieldAccess(receiver: Expr, fieldName: String): Unit =
    if (fieldName != "class") {
      receiver.exprType.foreach { rTpe =>
        toJavaClass(rTpe).foreach { clazz =>
          if (clazz.isArray && fieldName == "length") {
            // OK
          } else {
            val hasField = getAllFields(clazz).exists(_.getName == fieldName)
            if (!hasField) {
              throw AstValidationException(
                s"Field '$fieldName' not found on class '${clazz.getName}'.\nAvailable fields:\n${formatAvailableFields(clazz)}"
              )
            }
          }
        }
      }
    }

  def validateStaticField(tpe: TypeRef, fieldName: String): Unit =
    if (fieldName != "class") {
      toJavaClass(tpe).foreach { clazz =>
        val hasField = getAllFields(clazz).exists(_.getName == fieldName)
        if (!hasField) {
          throw AstValidationException(
            s"Static field '$fieldName' not found on class '${clazz.getName}'.\nAvailable fields:\n${formatAvailableFields(clazz)}"
          )
        }
      }
    }

// ─── Declarations ────────────────────────────────────────────────────────────

case class FieldDecl(
  name: String,
  tpe: TypeRef,
  modifiers: List[javax.lang.model.element.Modifier] = Nil
)

case class MethodDecl(
  name: String,
  returnType: TypeRef,
  parameters: List[Var],
  modifiers: List[javax.lang.model.element.Modifier] = Nil,
  body: Block[?]
)

object MethodDecl:
  def build[S](
    name: String,
    returnType: TypeRef,
    parameters: List[Var],
    modifiers: List[javax.lang.model.element.Modifier] = Nil
  )(body: BlockBuilder ?=> Block[S]): MethodDecl =
    val block = BlockBuilder.build(body)
    MethodDecl(name, returnType, parameters, modifiers, block)

case class ClassDecl(
  packageName: String,
  name: String,
  modifiers: List[javax.lang.model.element.Modifier] = Nil,
  superinterfaces: List[TypeRef] = Nil,
  fields: List[FieldDecl] = Nil,
  methods: List[MethodDecl] = Nil
)

