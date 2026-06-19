package me.bristermitten.mittenlib.codegen.dsl

import scala.collection.mutable

// ─── Match type: what block type do two branches merge into? ──────────────────

/** Type-level computation: merging two branch types.
  *
  * If both branches are [[Terminated]], the result is [[Terminated]]. Otherwise
  * it degrades to [[Open]] — you can't be certain which branch runs.
  *
  * This is the Scala 3 approximation of a Haskell type class instance
  * selection:
  * {{{
  *   instance Merge Terminated Terminated Terminated
  *   instance Merge _          _          Open
  * }}}
  */
type Merge[A, B] = (A, B) match
  case (Terminated, Terminated) => Terminated
  case _                        => Open

/** Builds a [[Block]] by sequencing statements.
  *
  * This is the "monadic" layer of the DSL. The key design:
  *
  *   - **Context functions** (`BlockBuilder ?=> Block[S]`) make the builder
  *     available as a given inside lambdas — you write `return_(x)` instead of
  *     `b.return_(x)`.
  *   - **`let`** returns `Block[S]` where `S` is inferred from the body's
  *     return type. One method handles both terminating and open cases — no
  *     `letOpen` split.
  *   - **`ifThenElse`** uses the [[Merge]] match type: when both branches are
  *     `Block[Terminated]`, the return type is `Block[Terminated]` — no
  *     Optional.
  *
  * Usage:
  * {{{
  *   val block = BlockBuilder.build {
  *     val data = declare(Types.DataTree, "data", contextGetData(ctx))
  *     ifThen(data.isNull) {
  *       return_(ResultExpr.ok(Expr.Null))
  *     }
  *     return_(ResultExpr.ok(data))
  *   }
  * }}}
  */
class BlockBuilder(private val names: NameGenerator):

  private val stmts = mutable.ListBuffer.empty[Statement]
  private var done = false
  private var terminator: Option[Terminator] = None

  // ─── Variable binding ───────────────────────────────────────────────────────

  /** Declare a variable and pass it to the body.
    *
    * The return type `Block[S]` is inferred from what the body returns:
    *   - body calls `return_(...)` → `S = Terminated`
    *   - body calls `buildOpen()` → `S = Open`
    *
    * No need for a separate `letOpen` method.
    */
  def let[S](tpe: TypeRef, value: Expr)(
      body: Var => BlockBuilder ?=> Block[S]
  ): Block[S] =
    let(tpe, None, value)(body)

  def let[S](tpe: TypeRef, hint: String, value: Expr)(
      body: Var => BlockBuilder ?=> Block[S]
  ): Block[S] =
    let(tpe, Some(hint), value)(body)

  private def let[S](tpe: TypeRef, hint: Option[String], value: Expr)(
      body: Var => BlockBuilder ?=> Block[S]
  ): Block[S] =
    checkOpen()
    val v = names.generate(tpe, hint)
    stmts += Statement.DeclareAssign(v, value)
    val child = BlockBuilder(names)
    val result = body(v)(using child)
    stmts ++= result.statements
    if result.isTerminated then done = true
    Block(stmts.toList, result.terminator)

  /** Declare a variable without a scoping lambda (flat style). Prefer [[let]]
    * for correctness — use this when multiple variables must be declared before
    * a natural termination point.
    */
  def declare(tpe: TypeRef, value: Expr): Var =
    declare(tpe, None, value)

  def declare(tpe: TypeRef, hint: String, value: Expr): Var =
    declare(tpe, Some(hint), value)

  private def declare(tpe: TypeRef, hint: Option[String], value: Expr): Var =
    checkOpen()
    val v = names.generate(tpe, hint)
    stmts += Statement.DeclareAssign(v, value)
    v

  // ─── Assignment ─────────────────────────────────────────────────────────────

  def assign(target: Var, value: Expr): Unit =
    checkOpen()
    stmts += Statement.Assign(target, value)

  def assignTo(target: Expr, value: Expr): Unit =
    checkOpen()
    stmts += Statement.Assign(target, value)

  // ─── Plain statements ────────────────────────────────────────────────────────

  def statement(expr: Expr): Unit =
    checkOpen()
    stmts += Statement.ExprStatement(expr)

  def blankLine(): Unit = stmts += Statement.BlankLine

  def comment(text: String): Unit = stmts += Statement.Comment(text)

  // ─── Control flow ───────────────────────────────────────────────────────────

  /** `if (cond) { body }` — body is an Open sub-block */
  def ifThen(cond: Expr)(body: BlockBuilder ?=> Unit): Unit =
    checkOpen()
    val inner = child()
    body(using inner)
    stmts += Statement.IfThen(cond, inner.buildOpen())

  /** `if (cond) { thenBody } else { elseBody }`
    *
    * The return type uses the [[Merge]] match type:
    *   - Both branches `Block[Terminated]` → returns `Block[Terminated]` (no
    *     Optional!)
    *   - Otherwise → returns `Block[Open]`
    *
    * This is the Scala 3 type-class approximation — the compiler resolves
    * [[Merge]] at the call site based on the inferred branch types.
    */
  def ifThenElse[A, B](cond: Expr)(thenBody: BlockBuilder ?=> Block[A])(
      elseBody: BlockBuilder ?=> Block[B]
  ): Block[Merge[A, B]] =
    checkOpen()
    val thenBuilder = child()
    val elseBuilder = child()
    val thenBlock = thenBody(using thenBuilder)
    val elseBlock = elseBody(using elseBuilder)
    stmts += Statement.IfThenElse(cond, thenBlock, elseBlock)

    val bothTerminate = thenBlock.isTerminated && elseBlock.isTerminated
    if bothTerminate then done = true

    // The Merge match type guarantees the terminator is present iff both branches terminate.
    // We use thenBlock's terminator as the representative (both branches are present in the AST).
    val terminator = if bothTerminate then thenBlock.terminator else None
    Block(stmts.toList, terminator).asInstanceOf[Block[Merge[A, B]]]

  /** `for (elementType element : iterable) { body }` */
  def forEach(tpe: TypeRef, iterable: Expr, hint: Option[String] = None)(
      body: Var => BlockBuilder ?=> Unit
  ): Unit =
    checkOpen()
    val element = names.generate(tpe, hint)
    val inner = child()
    body(element)(using inner)
    stmts += Statement.ForEach(element, iterable, inner.buildOpen())

  /** `try { tryBody } catch (ExType ex) { catchBody }` */
  def tryCatch(exType: TypeRef, hint: Option[String] = None)(
      tryBody: BlockBuilder ?=> Unit
  )(catchBody: Var => BlockBuilder ?=> Unit): Unit =
    checkOpen()
    val exVar = names.generate(exType, hint)
    val tryBuilder = child()
    val catchBuilder = child()
    tryBody(using tryBuilder)
    catchBody(exVar)(using catchBuilder)
    stmts += Statement.TryCatch(
      tryBuilder.buildOpen(),
      exType,
      exVar,
      catchBuilder.buildOpen()
    )

  // ─── Terminators ────────────────────────────────────────────────────────────

  def return_(value: Expr): Block[Terminated] =
    checkOpen()
    done = true
    val term = Some(Terminator.Return(value))
    this.terminator = term
    Block(stmts.toList, term)

  def returnVoid(): Block[Terminated] =
    checkOpen()
    done = true
    val term = Some(Terminator.ReturnVoid)
    this.terminator = term
    Block(stmts.toList, term)

  def throw_(expr: Expr): Block[Terminated] =
    checkOpen()
    done = true
    val term = Some(Terminator.Throw(expr))
    this.terminator = term
    Block(stmts.toList, term)

  // ─── Build ──────────────────────────────────────────────────────────────────

  def buildOpen(): Block[Open] = Block(stmts.toList, terminator)

  // ─── Internal ───────────────────────────────────────────────────────────────

  private def child(): BlockBuilder = BlockBuilder(names)

  private def checkOpen(): Unit =
    if done then
      throw IllegalStateException(
        s"Cannot add statements after block termination (${stmts.size} statements accumulated)"
      )

object BlockBuilder:
  /** Entry point: build a method body. The builder is available as a context
    * parameter.
    */
  def build[S](body: BlockBuilder ?=> Block[S]): Block[S] =
    val b = BlockBuilder(NameGenerator())
    body(using b)

  def buildOpen(body: BlockBuilder ?=> Unit): Block[Open] =
    val b = BlockBuilder(NameGenerator())
    body(using b)
    b.buildOpen()

  // Companion object helpers delegating to the context BlockBuilder
  def return_(value: Expr)(using b: BlockBuilder): Block[Terminated] =
    b.return_(value)

  def returnVoid()(using b: BlockBuilder): Block[Terminated] = b.returnVoid()

  def throw_(expr: Expr)(using b: BlockBuilder): Block[Terminated] =
    b.throw_(expr)

  def let[S](tpe: TypeRef, value: Expr)(body: Var => BlockBuilder ?=> Block[S])(
      using b: BlockBuilder
  ): Block[S] = b.let(tpe, value)(body)

  def let[S](tpe: TypeRef, hint: String, value: Expr)(
      body: Var => BlockBuilder ?=> Block[S]
  )(using b: BlockBuilder): Block[S] = b.let(tpe, hint, value)(body)

  def declare(tpe: TypeRef, value: Expr)(using b: BlockBuilder): Var =
    b.declare(tpe, value)

  def declare(tpe: TypeRef, hint: String, value: Expr)(using
      b: BlockBuilder
  ): Var = b.declare(tpe, hint, value)

  def assign(target: Var, value: Expr)(using b: BlockBuilder): Unit =
    b.assign(target, value)

  def assignTo(target: Expr, value: Expr)(using b: BlockBuilder): Unit =
    b.assignTo(target, value)

  def statement(expr: Expr)(using b: BlockBuilder): Unit = b.statement(expr)

  def blankLine()(using b: BlockBuilder): Unit = b.blankLine()

  def comment(text: String)(using b: BlockBuilder): Unit = b.comment(text)

  def ifThen(cond: Expr)(body: BlockBuilder ?=> Unit)(using
      b: BlockBuilder
  ): Unit = b.ifThen(cond)(body)

  def ifThenElse[A, B](cond: Expr)(thenBody: BlockBuilder ?=> Block[A])(
      elseBody: BlockBuilder ?=> Block[B]
  )(using b: BlockBuilder): Block[Merge[A, B]] =
    b.ifThenElse(cond)(thenBody)(elseBody)

  def forEach(tpe: TypeRef, iterable: Expr, hint: Option[String] = None)(
      body: Var => BlockBuilder ?=> Unit
  )(using b: BlockBuilder): Unit = b.forEach(tpe, iterable, hint)(body)

  def tryCatch(exType: TypeRef, hint: Option[String] = None)(
      tryBody: BlockBuilder ?=> Unit
  )(catchBody: Var => BlockBuilder ?=> Unit)(using b: BlockBuilder): Unit =
    b.tryCatch(exType, hint)(tryBody)(catchBody)

  def buildOpen()(using b: BlockBuilder): Block[Open] = b.buildOpen()
