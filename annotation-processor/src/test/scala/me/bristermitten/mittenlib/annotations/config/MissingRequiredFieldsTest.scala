package me.bristermitten.mittenlib.annotations.config

import com.google.testing.compile.Compilation
import com.google.testing.compile.CompilationSubject.assertThat as assertCompilation
import com.google.testing.compile.Compiler.javac
import com.google.testing.compile.JavaFileObjects
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class MissingRequiredFieldsTest extends AnyFunSuite with Matchers {

  test("testMissingRequiredFields") {
    val compilation: Compilation = javac()
      .withProcessors(new ConfigProcessor())
      .compile(
        JavaFileObjects.forSourceString(
          "me.bristermitten.mittenlib.tests.RequiredFieldsConfigDTO",
          """
          |package me.bristermitten.mittenlib.tests;
          |
          |import me.bristermitten.mittenlib.config.Config;
          |import me.bristermitten.mittenlib.config.Source;
          |import me.bristermitten.mittenlib.config.names.NamingPattern;
          |import me.bristermitten.mittenlib.config.names.NamingPatterns;
          |import org.jspecify.annotations.Nullable;
          |
          |@NamingPattern(NamingPatterns.LOWER_KEBAB_CASE)
          |@Source("required.yml")
          |@Config(requireDynamicInitialization = false)
          |public class RequiredFieldsConfigDTO {
          |    // Required primitive fields (no default values)
          |    public int requiredInt;
          |    public boolean requiredBoolean;
          |
          |    // Required object fields (no default values)
          |    public String requiredString;
          |
          |    // Optional fields (with default values)
          |    public double optionalDouble = 3.14;
          |    @Nullable public String optionalString = null;
          |
          |    // Required nested config
          |    public NestedConfigDTO nestedConfig;
          |
          |    @Config
          |    public static class NestedConfigDTO {
          |        // Required field in nested config
          |        public int requiredNestedInt;
          |
          |        // Optional field in nested config
          |        public String optionalNestedString = "default";
          |    }
          |}
          |""".stripMargin
        )
      )

    assertCompilation(compilation).succeeded()
    assertCompilation(compilation)
      .generatedSourceFile(
        "me.bristermitten.mittenlib.tests.RequiredFieldsConfig"
      )
      .isNotNull()

    assertCompilation(compilation)
      .generatedSourceFile(
        "me.bristermitten.mittenlib.tests.RequiredFieldsConfigDeserializer"
      )
      .contentsAsUtf8String()
      .contains("ConfigLoadingErrors.notFoundException(\"requiredInt\"")
  }
}
