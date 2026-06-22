package me.bristermitten.mittenlib.annotations.config

import com.google.testing.compile.Compilation
import com.google.testing.compile.CompilationSubject.assertThat as assertCompilation
import com.google.testing.compile.Compiler.javac
import com.google.testing.compile.JavaFileObjects
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class SerializationRequirementTest extends AnyFunSuite with Matchers {

  test("testSerializationRequirementFails") {
    val compilation: Compilation = javac()
      .withProcessors(new ConfigProcessor())
      .compile(
        JavaFileObjects.forSourceString(
          "me.bristermitten.mittenlib.tests.UnserializableConfigDTO",
          """
            |package me.bristermitten.mittenlib.tests;
            |
            |import me.bristermitten.mittenlib.config.Config;
            |
            |@Config(requireSerialization = true)
            |public class UnserializableConfigDTO {
            |    public Object unsupported;
            |}
            |""".stripMargin
        )
      )

    assertCompilation(compilation).failed()
    assertCompilation(compilation)
      .hadErrorContaining(
        "Serialization is required for this config, but it contains properties that cannot be serialized"
      )
  }

  test("testSerializationRequirementDefaultsToTrueAndFails") {
    val compilation: Compilation = javac()
      .withProcessors(new ConfigProcessor())
      .compile(
        JavaFileObjects.forSourceString(
          "me.bristermitten.mittenlib.tests.UnserializableDefaultConfigDTO",
          """
            |package me.bristermitten.mittenlib.tests;
            |
            |import me.bristermitten.mittenlib.config.Config;
            |
            |@Config
            |public class UnserializableDefaultConfigDTO {
            |    public Object unsupported;
            |}
            |""".stripMargin
        )
      )

    assertCompilation(compilation).failed()
    assertCompilation(compilation)
      .hadErrorContaining(
        "Serialization is required for this config, but it contains properties that cannot be serialized"
      )
  }

  test("testSerializationWarningWhenNotRequired") {
    val compilation: Compilation = javac()
      .withProcessors(new ConfigProcessor())
      .compile(
        JavaFileObjects.forSourceString(
          "me.bristermitten.mittenlib.tests.UnserializableWarningConfigDTO",
          """
            |package me.bristermitten.mittenlib.tests;
            |
            |import me.bristermitten.mittenlib.config.Config;
            |
            |@Config(requireSerialization = false)
            |public class UnserializableWarningConfigDTO {
            |    public Object unsupported;
            |}
            |""".stripMargin
        )
      )

    assertCompilation(compilation).succeeded()
    assertCompilation(compilation)
      .hadWarningContaining(
        "This config contains properties that cannot be serialized"
      )
  }
}
