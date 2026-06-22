package me.bristermitten.mittenlib.annotations.config

import com.google.testing.compile.Compilation
import com.google.testing.compile.CompilationSubject.assertThat as assertCompilation
import com.google.testing.compile.Compiler.javac
import com.google.testing.compile.JavaFileObjects
import io.toolisticon.cute.Cute
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class OverriddenNameGeneratorTest extends AnyFunSuite with Matchers {

  test("generateFullConfigClassName") {
    val compilation: Compilation = javac()
      .withProcessors(new ConfigProcessor())
      .compile(
        JavaFileObjects.forSourceString(
          "me.bristermitten.mittenlib.tests.OverriddenNameDTO",
          """
            |package me.bristermitten.mittenlib.tests;
            |import java.util.Map;
            |import me.bristermitten.mittenlib.config.*;
            |@Config
            |public class OverriddenNameDTO {
            |    public int clone;
            |}
            |""".stripMargin
        )
      )

    assertCompilation(compilation).succeeded()
  }

  test("testOverriddenConfigName") {
    Cute
      .blackBoxTest()
      .`given`()
      .processor(classOf[ConfigProcessor])
      .andSourceFile(
        "OverriddenImplConfig",
        """
          |import me.bristermitten.mittenlib.config.Config;
          |@Config(className = "ThisIsTheImpl")
          |public interface OverriddenImplConfig {
          |    int id();
          |}
          |""".stripMargin
      )
      .whenCompiled()
      .thenExpectThat()
      .compilationSucceeds()
      .andThat()
      .generatedClass("ThisIsTheImpl")
      .exists()
      .executeTest()
  }

  test("testNestedOverriddenConfigName") {
    Cute
      .blackBoxTest()
      .`given`()
      .processor(classOf[ConfigProcessor])
      .andSourceFile(
        "OverriddenImplConfig",
        """
          |import me.bristermitten.mittenlib.config.Config;
          |
          |@Config(className = "ThisIsTheImpl")
          |public interface OverriddenImplConfig {
          |    int id();
          |
          |    @Config
          |    interface NormalSubConfig {
          |        int id2();
          |    }
          |}
          |""".stripMargin
      )
      .whenCompiled()
      .thenExpectThat()
      .compilationSucceeds()
      .andThat()
      .generatedClass("ThisIsTheImpl")
      .exists()
      .executeTest()
  }
}
