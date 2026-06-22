package me.bristermitten.mittenlib.codegen.dsl

import com.palantir.javapoet.CodeBlock

import scala.jdk.CollectionConverters.*

/** Compiles a [[Block]] AST into a JavaPoet [[CodeBlock]].
  *
  * This is the *only* place in the DSL that touches JavaPoet's CodeBlock API.
  * All other layers work purely with the AST data types.
  */
object CodeBlockRenderer:

  def render(block: Block[?]): CodeBlock =
    val builder = CodeBlock.builder()
    block.statements.foreach(renderStatement(_, builder))
    block.terminator.foreach(renderTerminator(_, builder))
    builder.build()

  // ─── Statements ─────────────────────────────────────────────────────────────

  private def renderStatement(stmt: Statement, b: CodeBlock.Builder): Unit =
    stmt match
      case Statement.DeclareAssign(v, value) =>
        b.addStatement(
          "$T $L = $L",
          v.tpe.toTypeName,
          v.generatedName,
          renderExpr(value)
        )

      case Statement.Assign(target, value) =>
        b.addStatement("$L = $L", renderExpr(target), renderExpr(value))

      case Statement.ExprStatement(expr) =>
        b.addStatement("$L", renderExpr(expr))

      case Statement.IfThen(cond, body) =>
        b.beginControlFlow("if ($L)", renderExpr(cond))
        appendBlock(body, b)
        b.endControlFlow()

      case Statement.IfThenElse(cond, thenBlock, elseBlock) =>
        b.beginControlFlow("if ($L)", renderExpr(cond))
        appendBlock(thenBlock, b)
        b.nextControlFlow("else")
        appendBlock(elseBlock, b)
        b.endControlFlow()

      case Statement.ForEach(element, iterable, body) =>
        b.beginControlFlow(
          "for ($T $L : $L)",
          element.tpe.toTypeName,
          element.generatedName,
          renderExpr(iterable)
        )
        appendBlock(body, b)
        b.endControlFlow()

      case Statement.ForLoop(init, cond, update, body) =>
        val initCode = renderStatementInline(init)
        b.beginControlFlow(
          "for ($L; $L; $L)",
          initCode,
          renderExpr(cond),
          renderExpr(update)
        )
        appendBlock(body, b)
        b.endControlFlow()

      case Statement.TryCatch(tryBody, exType, exVar, catchBody) =>
        b.beginControlFlow("try")
        appendBlock(tryBody, b)
        b.nextControlFlow(
          "catch ($T $L)",
          exType.toTypeName,
          exVar.generatedName
        )
        appendBlock(catchBody, b)
        b.endControlFlow()

      case Statement.BlankLine  => b.add("\n")
      case Statement.Comment(t) => b.add("// $L\n", t)

  private def renderStatementInline(stmt: Statement): CodeBlock = stmt match
    case Statement.DeclareAssign(v, value) =>
      CodeBlock.of(
        "$T $L = $L",
        v.tpe.toTypeName,
        v.generatedName,
        renderExpr(value)
      )
    case Statement.Assign(target, value) =>
      CodeBlock.of("$L = $L", renderExpr(target), renderExpr(value))
    case _ =>
      throw IllegalArgumentException(
        s"Unsupported for-loop init: ${stmt.getClass.getSimpleName}"
      )

  // ─── Terminators ────────────────────────────────────────────────────────────

  private def renderTerminator(term: Terminator, b: CodeBlock.Builder): Unit =
    term match
      case Terminator.Return(v)  => b.addStatement("return $L", renderExpr(v))
      case Terminator.ReturnVoid => b.addStatement("return")
      case Terminator.Throw(e)   => b.addStatement("throw $L", renderExpr(e))

  // ─── Expressions ────────────────────────────────────────────────────────────

  def renderExpr(expr: Expr[?]): CodeBlock = expr match
    case Expr.Literal(r) => CodeBlock.of("$L", r)
    case Expr.Null       => CodeBlock.of("null")
    case Expr.BoolLit(v) => CodeBlock.of("$L", v.toString)
    case Expr.This       => CodeBlock.of("this")
    case Expr.Super      => CodeBlock.of("super")
    case v: Var[?]       => CodeBlock.of("$L", v.generatedName)

    case Expr.MethodCall(recv, method, args) =>
      val argsBlock = joinExprs(args)
      CodeBlock.of("$L.$L($L)", renderExpr(recv), method, argsBlock)

    case Expr.StaticCall(tpe, method, args) =>
      val argsBlock = joinExprs(args)
      CodeBlock.of("$T.$L($L)", tpe.toTypeName, method, argsBlock)

    case Expr.FieldAccess(recv, f) =>
      CodeBlock.of("$L.$L", renderExpr(recv), f)

    case Expr.StaticField(tpe, f) =>
      CodeBlock.of("$T.$L", tpe.toTypeName, f)

    case Expr.MethodRef(recv, method) =>
      CodeBlock.of("$L::$L", renderExpr(recv), method)

    case Expr.StaticMethodRef(tpe, method) =>
      CodeBlock.of("$T::$L", tpe.toTypeName, method)

    case Expr.NewInstance(tpe, args) =>
      CodeBlock.of("new $T($L)", tpe.toTypeName, joinExprs(args))

    case Expr.NewAnonymousInstance(tpe, args) =>
      CodeBlock.of("new $T($L) {}", tpe.toTypeName, joinExprs(args))

    case Expr.Cast(tpe, e) =>
      val targetTypeName = tpe.toTypeName
      if (targetTypeName.isPrimitive) {
        CodeBlock.of("(($T) $L)", targetTypeName, renderExpr(e))
      } else {
        CodeBlock.of("(($T) (Object) $L)", targetTypeName, renderExpr(e))
      }

    case Expr.InstanceOf(e, tpe) =>
      CodeBlock.of("$L instanceof $T", renderExpr(e), tpe.toTypeName)

    case Expr.Ternary(cond, t, f) =>
      CodeBlock.of(
        "($L ? $L : $L)",
        renderExpr(cond),
        renderExpr(t),
        renderExpr(f)
      )

    case Expr.BinaryOp(l, op, r) =>
      CodeBlock.of("$L $L $L", renderExpr(l), op, renderExpr(r))

    case Expr.UnaryOp(op, operand) =>
      operand match {
        case _: Var[?] | _: Expr.Literal | _: Expr.BoolLit | Expr.Null |
            Expr.This | Expr.Super =>
          CodeBlock.of("$L$L", op, renderExpr(operand))
        case _ =>
          CodeBlock.of("$L($L)", op, renderExpr(operand))
      }

    case Expr.Lambda(params, body) =>
      val ps = params.map(_.generatedName).mkString(", ")
      CodeBlock.of("($L) -> {\n$L}", ps, renderNested(body))

    case Expr.LambdaExpr(params, body) =>
      val ps = params.map(_.generatedName).mkString(", ")
      CodeBlock.of("($L) -> $L", ps, renderExpr(body))

  // ─── Helpers ────────────────────────────────────────────────────────────────

  def renderNested(block: Block[?]): CodeBlock =
    val builder = CodeBlock.builder()
    block.statements.foreach(renderStatementNested(_, builder))
    block.terminator.foreach(renderTerminatorNested(_, builder))
    builder.build()

  private def appendBlock(block: Block[?], b: CodeBlock.Builder): Unit =
    block.statements.foreach(renderStatement(_, b))
    block.terminator.foreach(renderTerminator(_, b))

  private def appendBlockNested(block: Block[?], b: CodeBlock.Builder): Unit =
    block.statements.foreach(renderStatementNested(_, b))
    block.terminator.foreach(renderTerminatorNested(_, b))

  private def renderStatementNested(
      stmt: Statement,
      b: CodeBlock.Builder
  ): Unit = stmt match
    case Statement.DeclareAssign(v, value) =>
      b.add(
        "$T $L = $L;\n",
        v.tpe.toTypeName,
        v.generatedName,
        renderExpr(value)
      )

    case Statement.Assign(target, value) =>
      b.add("$L = $L;\n", renderExpr(target), renderExpr(value))

    case Statement.ExprStatement(expr) =>
      b.add("$L;\n", renderExpr(expr))

    case Statement.IfThen(cond, body) =>
      b.add("if ($L) {\n", renderExpr(cond))
      b.indent()
      appendBlockNested(body, b)
      b.unindent()
      b.add("}\n")

    case Statement.IfThenElse(cond, thenBlock, elseBlock) =>
      b.add("if ($L) {\n", renderExpr(cond))
      b.indent()
      appendBlockNested(thenBlock, b)
      b.unindent()
      b.add("} else {\n")
      b.indent()
      appendBlockNested(elseBlock, b)
      b.unindent()
      b.add("}\n")

    case Statement.ForEach(element, iterable, body) =>
      b.add(
        "for ($T $L : $L) {\n",
        element.tpe.toTypeName,
        element.generatedName,
        renderExpr(iterable)
      )
      b.indent()
      appendBlockNested(body, b)
      b.unindent()
      b.add("}\n")

    case Statement.ForLoop(init, cond, update, body) =>
      val initCode = renderStatementInline(init)
      b.add(
        "for ($L; $L; $L) {\n",
        initCode,
        renderExpr(cond),
        renderExpr(update)
      )
      b.indent()
      appendBlockNested(body, b)
      b.unindent()
      b.add("}\n")

    case Statement.TryCatch(tryBody, exType, exVar, catchBody) =>
      b.add("try {\n")
      b.indent()
      appendBlockNested(tryBody, b)
      b.unindent()
      b.add("} catch ($T $L) {\n", exType.toTypeName, exVar.generatedName)
      b.indent()
      appendBlockNested(catchBody, b)
      b.unindent()
      b.add("}\n")

    case Statement.BlankLine  => b.add("\n")
    case Statement.Comment(t) => b.add("// $L\n", t)

  private def renderTerminatorNested(
      term: Terminator,
      b: CodeBlock.Builder
  ): Unit = term match
    case Terminator.Return(v)  => b.add("return $L;\n", renderExpr(v))
    case Terminator.ReturnVoid => b.add("return;\n")
    case Terminator.Throw(e)   => b.add("throw $L;\n", renderExpr(e))

  private def joinExprs(args: List[Expr[?]]): CodeBlock =
    args
      .map(renderExpr)
      .asJava
      .stream()
      .collect(CodeBlock.joining(", "))

  // ─── Declaration rendering ──────────────────────────────────────────────────

  import com.palantir.javapoet.{FieldSpec, MethodSpec, TypeSpec, JavaFile}

  def renderMethod(decl: MethodDecl): MethodSpec =
    val builder = MethodSpec
      .methodBuilder(decl.name)
      .addModifiers(decl.modifiers*)
      .returns(decl.returnType.toTypeName)
    for (ann <- decl.annotations) {
      builder.addAnnotation(ann)
    }
    for (param <- decl.parameters) {
      builder.addParameter(param.tpe.toTypeName, param.generatedName)
    }
    val bodyBlock = render(decl.body)
    builder.addCode(bodyBlock)
    builder.build()

  def renderConstructor(decl: ConstructorDecl): MethodSpec =
    val builder = MethodSpec
      .constructorBuilder()
      .addModifiers(decl.modifiers*)
    for (ann <- decl.annotations) {
      builder.addAnnotation(ann)
    }
    for (param <- decl.parameters) {
      builder.addParameter(param.tpe.toTypeName, param.generatedName)
    }
    val bodyBlock = render(decl.body)
    builder.addCode(bodyBlock)
    builder.build()

  def renderField(field: FieldDecl): FieldSpec =
    val builder =
      FieldSpec.builder(field.tpe.toTypeName, field.name, field.modifiers*)
    for (ann <- field.annotations) {
      builder.addAnnotation(ann)
    }
    field.javadoc.foreach(text => builder.addJavadoc(text))
    field.initializer.foreach(init => builder.initializer(renderExpr(init)))
    builder.build()

  def renderClass(decl: ClassDecl): TypeSpec =
    val builder = TypeSpec
      .classBuilder(decl.name)
      .addModifiers(decl.modifiers*)
    for (ann <- decl.annotations) {
      builder.addAnnotation(ann)
    }
    decl.superclass.foreach(sc => builder.superclass(sc.toTypeName))
    for (superinterface <- decl.superinterfaces) {
      builder.addSuperinterface(superinterface.toTypeName)
    }
    for (constructor <- decl.constructors) {
      builder.addMethod(renderConstructor(constructor))
    }
    for (field <- decl.fields) {
      builder.addField(renderField(field))
    }
    for (method <- decl.methods) {
      builder.addMethod(renderMethod(method))
    }
    for (nested <- decl.nestedTypes) {
      builder.addType(renderClass(nested))
    }
    for (extra <- decl.extraMethods) {
      builder.addMethod(extra)
    }
    builder.build()

  def renderJavaFile(decl: ClassDecl): JavaFile =
    JavaFile
      .builder(decl.packageName, renderClass(decl))
      .skipJavaLangImports(true)
      .build()
