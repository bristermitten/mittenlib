package me.bristermitten.mittenlib.annotations.config

import com.google.testing.compile.Compilation
import com.google.testing.compile.CompilationSubject.assertThat as assertCompilation
import com.google.testing.compile.Compiler.javac
import com.google.testing.compile.JavaFileObjects
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class EmptyConfigGeneratorTest extends AnyFunSuite with Matchers {

  test("generateFullConfigClassName") {
    val compilation: Compilation = javac()
      .withProcessors(new ConfigProcessor())
      .compile(
        JavaFileObjects.forSourceString(
          "me.bristermitten.mittenlib.tests.EmptyConfigDTO",
          """
          |package me.bristermitten.mittenlib.tests;
          |import java.util.Map;
          |import me.bristermitten.mittenlib.config.*;
          |@Config
          |public class EmptyConfigDTO {
          |}
          |""".stripMargin
        )
      )

    assertCompilation(compilation).succeeded()
  }
}
