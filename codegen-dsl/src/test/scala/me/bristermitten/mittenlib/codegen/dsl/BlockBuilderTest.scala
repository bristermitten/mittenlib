package me.bristermitten.mittenlib.codegen.dsl

import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.*
import BlockBuilder.*

class BlockBuilderTest:

  @Test def simpleReturn(): Unit =
    val block: Block[Terminated] = BlockBuilder.build {
      return_(ResultExpr.ok(Expr.Null))
    }
    val rendered = CodeBlockRenderer.render(block).toString.trim
    assertThat(rendered).isEqualTo("return me.bristermitten.mittenlib.util.Result.ok(null);")

  @Test def letBinding_inferredAsTerminated(): Unit =
    // The type Block[Terminated] is inferred from the body's return type
    val block: Block[Terminated] = BlockBuilder.build {
      let(Types.String, "value", Expr.str("hello")) { value =>
        return_(ResultExpr.ok(value))
      }
    }
    val rendered = CodeBlockRenderer.render(block).toString
    assertThat(rendered).contains("String value = \"hello\"")
    assertThat(rendered).contains("return")

  @Test def letBinding_inferredAsOpen(): Unit =
    // No return → Block[Open] inferred
    val block: Block[Open] = BlockBuilder.build {
      let(Types.String, "value", Expr.str("hello")) { value =>
        statement(value.call("toUpperCase"))
        buildOpen()
      }
    }
    val rendered = CodeBlockRenderer.render(block).toString
    assertThat(rendered).contains("String value = \"hello\"")
    assertThat(rendered).contains("value.toUpperCase()")

  @Test def declare_uniqueNames(): Unit =
    val block: Block[Open] = BlockBuilder.buildOpen {
      val v1 = declare(Types.String, "result", Expr.str("a"))
      val v2 = declare(Types.String, "result", Expr.str("b"))
      assertThat(v1.generatedName).isEqualTo("result")
      assertThat(v2.generatedName).isEqualTo("result1")
    }

  @Test def cannotAddAfterReturn(): Unit =
    try
      BlockBuilder.buildOpen {
        return_(Expr.Null)
        statement(Expr.Null)  // should throw
      }
      fail("Should have thrown IllegalStateException")
    catch
      case e: IllegalStateException =>
        assertThat(e.getMessage).contains("termination")

  @Test def ifThenElse_bothTerminate_returnsTerminated(): Unit =
    // Compiler infers Block[Terminated] — no Optional
    val block: Block[Terminated] = BlockBuilder.build {
      ifThenElse(Expr.bool(true)) {
        return_(Expr.int(1))
      } {
        return_(Expr.int(2))
      }
    }
    val rendered = CodeBlockRenderer.render(block).toString
    assertThat(rendered).contains("if (true)")
    assertThat(rendered).contains("return 1")
    assertThat(rendered).contains("return 2")

  @Test def ifThenElse_oneBranchOpen_returnsOpen(): Unit =
    // When branches disagree, Merge[Terminated, Open] = Open
    val block: Block[Open] = BlockBuilder.build {
      ifThenElse(Expr.bool(true)) {
        return_(Expr.int(1))
      } {
        statement(Expr.int(2))
        buildOpen()
      }
    }
    // Can still add more after — block is open
    val rendered = CodeBlockRenderer.render(block).toString
    assertThat(rendered).contains("if (true)")

  @Test def nestedBuilders_shareNameGenerator(): Unit =
    BlockBuilder.buildOpen {
      val outer = declare(Types.String, "item", Expr.str("outer"))
      ifThen(Expr.bool(true)) {
        val inner = declare(Types.String, "item", Expr.str("inner"))
        // Inner should get "item1", not "item" (would collide)
        assertThat(inner.generatedName).isNotEqualTo(outer.generatedName)
      }
    }

  @Test def tryCatch_rendersCorrectly(): Unit =
    val block: Block[Terminated] = BlockBuilder.build {
      tryCatch(Types.Exception) {
        statement(Expr.staticCall(Types.String, "valueOf", Expr.int(1)))
      } { ex =>
        statement(ResultExpr.fail(ex))
      }
      return_(ResultExpr.ok(Expr.Null))
    }
    val rendered = CodeBlockRenderer.render(block).toString
    assertThat(rendered).contains("try {")
    assertThat(rendered).contains("catch (")
    assertThat(rendered).contains("Exception")

  @Test def forEach_rendersCorrectly(): Unit =
    val block: Block[Open] = BlockBuilder.buildOpen {
      val items = declare(Types.List, "items", Expr.Null)
      forEach(Types.String, items) { item =>
        statement(item.call("toUpperCase"))
      }
    }
    val rendered = CodeBlockRenderer.render(block).toString
    assertThat(rendered).contains("for (")
    assertThat(rendered).contains("String")
    assertThat(rendered).contains("toUpperCase()")

  @Test def operatorSyntax_onExpr(): Unit =
    val expr = Expr.bool(true) && Expr.bool(false)
    val rendered = CodeBlockRenderer.renderExpr(expr).toString
    assertThat(rendered).isEqualTo("true && false")

  @Test def contextFunction_noExplicitBuilderRef(): Unit =
    // The whole point — no `b.` prefix needed anywhere
    val block: Block[Terminated] = BlockBuilder.build {
      val x = declare(Types.String, "x", Expr.str("hello"))
      ifThen(x.isNull) {
        // still no b.return_, just return_
        return_(ResultExpr.okNull)
      }
      return_(ResultExpr.ok(x))
    }
    assertThat(block.isTerminated).isTrue
