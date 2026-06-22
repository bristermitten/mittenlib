package me.bristermitten.mittenlib.annotations.config

import com.google.testing.compile.Compilation
import com.google.testing.compile.CompilationSubject.assertThat as assertCompilation
import com.google.testing.compile.Compiler.javac
import com.google.testing.compile.JavaFileObjects
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class NullableFieldsDeserializationTest extends AnyFunSuite with Matchers {

  test("testNullableFieldsDeserialization") {
    val compilation: Compilation = javac()
      .withProcessors(new ConfigProcessor())
      .compile(
        JavaFileObjects.forSourceString(
          "me.bristermitten.mittenlib.tests.NullableFieldsConfigDTO",
          """
            |package me.bristermitten.mittenlib.tests;
            |
            |import me.bristermitten.mittenlib.config.Config;
            |import me.bristermitten.mittenlib.config.Source;
            |import me.bristermitten.mittenlib.config.names.NamingPattern;
            |import me.bristermitten.mittenlib.config.names.NamingPatterns;
            |import org.jspecify.annotations.Nullable;
            |
            |import java.util.List;
            |import java.util.Map;
            |
            |@NamingPattern(NamingPatterns.LOWER_KEBAB_CASE)
            |@Source("nullable.yml")
            |@Config
            |public class NullableFieldsConfigDTO {
            |    // Required fields (not nullable)
            |    public int requiredInt = 42;
            |    public String requiredString = "Required";
            |
            |    // Nullable primitive wrappers
            |    @Nullable public Integer nullableInt = null;
            |    @Nullable public Double nullableDouble = null;
            |    @Nullable public Boolean nullableBoolean = null;
            |
            |    // Nullable objects
            |    @Nullable public String nullableString = null;
            |    @Nullable public List<String> nullableList = null;
            |    @Nullable public Map<String, Integer> nullableMap = null;
            |
            |    // Nullable nested config
            |    @Nullable public NestedConfigDTO nullableNestedConfig = null;
            |
            |    @Config
            |    public static class NestedConfigDTO {
            |        public int value = 100;
            |        @Nullable public String description = null;
            |    }
            |}
            |""".stripMargin
        )
      )

    assertCompilation(compilation).succeeded()

    // Verify that the generated class exists
    assertCompilation(compilation)
      .generatedSourceFile(
        "me.bristermitten.mittenlib.tests.NullableFieldsConfig"
      )
      .isNotNull()

    // Verify that the generated nested class exists
    assertCompilation(compilation)
      .generatedSourceFile(
        "me.bristermitten.mittenlib.tests.NullableFieldsConfig"
      )
      .contentsAsUtf8String()
      .contains("static class NestedConfig")
  }
}
