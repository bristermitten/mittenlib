package me.bristermitten.mittenlib.annotations.config

import com.google.testing.compile.Compilation
import com.google.testing.compile.CompilationSubject.assertThat as assertCompilation
import com.google.testing.compile.Compiler.javac
import com.google.testing.compile.JavaFileObjects
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class ToStringGeneratorTest extends AnyFunSuite with Matchers {

  test("generatesToStringMethod") {
    val compilation: Compilation = javac()
      .withProcessors(new ConfigProcessor())
      .compile(
        JavaFileObjects.forSourceString(
          "me.bristermitten.mittenlib.tests.ToStringConfigDTO",
          """
            |package me.bristermitten.mittenlib.tests;
            |import java.util.Map;
            |import me.bristermitten.mittenlib.config.*;import me.bristermitten.mittenlib.config.generate.GenerateToString;
            |@Config
            |@GenerateToString
            |public class ToStringConfigDTO {
            |    int x = 3;
            |    int y;
            |    String z;
            |}
            |""".stripMargin
        )
      )

    assertCompilation(compilation).succeeded()
    assertCompilation(compilation)
      .generatedSourceFile("me.bristermitten.mittenlib.tests.ToStringConfig")
      .isNotNull()
    assertCompilation(compilation)
      .generatedSourceFile("me.bristermitten.mittenlib.tests.ToStringConfig")
      .contentsAsUtf8String()
      .contains(
        """  @Override
          |  public String toString() {
          |    return "ToStringConfig{" + "x=" + x + "," + "y=" + y + "," + "z=" + z + "}";
          |  }""".stripMargin
      )
  }

  test("generatesToStringMethodWithSubclass") {
    val compilation: Compilation = javac()
      .withProcessors(new ConfigProcessor())
      .compile(
        JavaFileObjects.forSourceString(
          "me.bristermitten.mittenlib.tests.ToStringConfigDTO",
          """
            |package me.bristermitten.mittenlib.tests;
            |
            |import me.bristermitten.mittenlib.config.Config;
            |import me.bristermitten.mittenlib.config.generate.GenerateToString;
            |
            |@Config
            |@GenerateToString
            |public class ToStringConfigDTO {
            |    int x = 3;
            |
            |    @Config
            |    public static class SubclassDTO {
            |        int y = 4;
            |    }
            |}
            |""".stripMargin
        )
      )

    assertCompilation(compilation).succeeded()
    assertCompilation(compilation)
      .generatedSourceFile("me.bristermitten.mittenlib.tests.ToStringConfig")
      .isNotNull()
    assertCompilation(compilation)
      .generatedSourceFile("me.bristermitten.mittenlib.tests.ToStringConfig")
      .contentsAsUtf8String()
      .containsMatch(
        """(?s)\s+@Override\s+public String toString\(\) \{\s+return "Subclass\{" \+ "y=" \+ y \+ "}";\s+}"""
      )
  }
}
