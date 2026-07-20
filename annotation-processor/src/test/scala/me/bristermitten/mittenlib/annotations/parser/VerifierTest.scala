package me.bristermitten.mittenlib.annotations.parser

import com.google.testing.compile.Compilation
import com.google.testing.compile.CompilationSubject.assertThat as assertCompilation
import com.google.testing.compile.Compiler.javac
import com.google.testing.compile.JavaFileObjects
import me.bristermitten.mittenlib.annotations.config.ConfigProcessor
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class VerifierTest extends AnyFunSuite with Matchers {

  test("testUnionAlternativeNotExtendingUnion") {
    val compilation: Compilation = javac()
      .withProcessors(new ConfigProcessor())
      .compile(
        JavaFileObjects.forSourceString(
          "me.bristermitten.mittenlib.tests.UnionConfigDTO",
          """
            |package me.bristermitten.mittenlib.tests;
            |import me.bristermitten.mittenlib.config.*;
            |
            |@Config
            |@ConfigUnion
            |public class UnionConfigDTO {
            |    public int sharedProperty;
            |
            |    @Config
            |    public static class AlternativeOneDTO {
            |        // Missing 'extends UnionConfigDTO'
            |        public String altProperty;
            |    }
            |}
            |""".stripMargin
        )
      )

    assertCompilation(compilation).failed()
    assertCompilation(compilation).hadErrorContaining(
      "UnionConfigDTO MUST extend the union type"
    )
  }

  test("testEnumParsingSchemeNotEnumWarning") {
    val compilation: Compilation = javac()
      .withProcessors(new ConfigProcessor())
      .compile(
        JavaFileObjects.forSourceString(
          "me.bristermitten.mittenlib.tests.EnumWarnConfigDTO",
          """
            |package me.bristermitten.mittenlib.tests;
            |import me.bristermitten.mittenlib.config.*;
            |import me.bristermitten.mittenlib.config.names.*;
            |
            |@Config
            |public class EnumWarnConfigDTO {
            |    @EnumParsingScheme(EnumParsingSchemes.EXACT_MATCH)
            |    public String notAnEnumField;
            |}
            |""".stripMargin
        )
      )

    assertCompilation(compilation).succeeded()
    assertCompilation(compilation)
      .hadWarningContaining(
        "This property's type is not an enum, so the @EnumParsingScheme annotation will have no effect"
      )
  }

  test("testClassDtoMissingNoArgConstructor") {
    val compilation: Compilation = javac()
      .withProcessors(new ConfigProcessor())
      .compile(
        JavaFileObjects.forSourceString(
          "me.bristermitten.mittenlib.tests.NoArgMissingConfigDTO",
          """
            |package me.bristermitten.mittenlib.tests;
            |import me.bristermitten.mittenlib.config.*;
            |
            |@Config(requireDynamicInitialization = false)
            |public class NoArgMissingConfigDTO {
            |    public int level = 5;
            |
            |    private NoArgMissingConfigDTO(int level) {
            |        this.level = level;
            |    }
            |}
            |""".stripMargin
        )
      )

    assertCompilation(compilation).failed()
    assertCompilation(compilation)
      .hadErrorContaining(
        "has fields with default values, but is missing an accessible (non-private) zero-arguments constructor"
      )
  }

  test("testConstraintTypeMismatch") {
    val compilation: Compilation = javac()
      .withProcessors(new ConfigProcessor())
      .compile(
        JavaFileObjects.forSourceString(
          "me.bristermitten.mittenlib.tests.ConstraintMismatchConfigDTO",
          """
            |package me.bristermitten.mittenlib.tests;
            |import me.bristermitten.mittenlib.config.*;
            |import me.bristermitten.mittenlib.config.validation.*;
            |
            |@Config
            |public class ConstraintMismatchConfigDTO {
            |    @NotBlank public int invalidNotBlankOnInt;
            |
            |    @Positive public String invalidPositiveOnString;
            |}
            |""".stripMargin
        )
      )

    assertCompilation(compilation).failed()
    assertCompilation(compilation)
      .hadErrorContaining(
        "Constraint annotation @NotBlank cannot be applied to type int. Expected a String or CharSequence"
      )
    assertCompilation(compilation)
      .hadErrorContaining(
        "Constraint annotation @Positive cannot be applied to type java.lang.String. Expected a numeric type"
      )
  }

  test("testSerializationNotSupported") {
    val compilation: Compilation = javac()
      .withProcessors(new ConfigProcessor())
      .compile(
        JavaFileObjects.forSourceString(
          "me.bristermitten.mittenlib.tests.UnserializableConfigDTO",
          """
            |package me.bristermitten.mittenlib.tests;
            |import me.bristermitten.mittenlib.config.*;
            |
            |@Config(requireSerialization = true)
            |public class UnserializableConfigDTO {
            |    public java.lang.Thread unsupportedField;
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

  test("testSerializationNotSupportedWarning") {
    val compilation: Compilation = javac()
      .withProcessors(new ConfigProcessor())
      .compile(
        JavaFileObjects.forSourceString(
          "me.bristermitten.mittenlib.tests.UnserializableWarnConfigDTO",
          """
            |package me.bristermitten.mittenlib.tests;
            |import me.bristermitten.mittenlib.config.*;
            |
            |@Config(requireSerialization = false)
            |public class UnserializableWarnConfigDTO {
            |    public java.lang.Thread unsupportedField;
            |}
            |""".stripMargin
        )
      )

    assertCompilation(compilation).succeeded()
    assertCompilation(compilation).hadWarningContaining(
      "This config contains properties that cannot be serialized"
    )
  }

  test("testNotDynamicallyInitializableError") {
    val compilation: Compilation = javac()
      .withProcessors(new ConfigProcessor())
      .compile(
        JavaFileObjects.forSourceString(
          "me.bristermitten.mittenlib.tests.NotDynInitConfigDTO",
          """
            |package me.bristermitten.mittenlib.tests;
            |import me.bristermitten.mittenlib.config.*;
            |
            |@Source("config.yml")
            |@Config(requireDynamicInitialization = true)
            |public class NotDynInitConfigDTO {
            |    public String requiredFieldNoDefault;
            |}
            |""".stripMargin
        )
      )

    assertCompilation(compilation).failed()
    assertCompilation(compilation)
      .hadErrorContaining(
        "has a @Source but is not dynamically initializable because the following required properties lack default values"
      )
  }

  test("testNotDynamicallyInitializableWarning") {
    val compilation: Compilation = javac()
      .withProcessors(new ConfigProcessor())
      .compile(
        JavaFileObjects.forSourceString(
          "me.bristermitten.mittenlib.tests.NotDynInitWarnConfigDTO",
          """
            |package me.bristermitten.mittenlib.tests;
            |import me.bristermitten.mittenlib.config.*;
            |
            |@Source("config.yml")
            |@Config(requireDynamicInitialization = false)
            |public class NotDynInitWarnConfigDTO {
            |    public String requiredFieldNoDefault;
            |}
            |""".stripMargin
        )
      )

    assertCompilation(compilation).succeeded()
    assertCompilation(compilation)
      .hadWarningContaining(
        "has a @Source but is not dynamically initializable because the following required properties lack default values"
      )
  }

  test("testUnionAlternativeExtendingOtherClassNotUnion") {
    val compilation: Compilation = javac()
      .withProcessors(new ConfigProcessor())
      .compile(
        JavaFileObjects.forSourceString(
          "me.bristermitten.mittenlib.tests.UnionConfigDTO",
          """
            |package me.bristermitten.mittenlib.tests;
            |import me.bristermitten.mittenlib.config.*;
            |
            |@Config
            |@ConfigUnion
            |public class UnionConfigDTO {
            |    public int sharedProperty;
            |
            |    public static class SomeOtherClass {}
            |
            |    @Config
            |    public static class AlternativeOneDTO extends SomeOtherClass {
            |        public String altProperty;
            |    }
            |}
            |""".stripMargin
        )
      )

    assertCompilation(compilation).failed()
    assertCompilation(compilation).hadErrorContaining(
      "UnionConfigDTO MUST extend the union type"
    )
  }

  test("testTransientMethodNotDefaultError") {
    val compilation: Compilation = javac()
      .withProcessors(new ConfigProcessor())
      .compile(
        JavaFileObjects.forSourceString(
          "me.bristermitten.mittenlib.tests.TransientAbstractConfigDTO",
          """
            |package me.bristermitten.mittenlib.tests;
            |import me.bristermitten.mittenlib.config.*;
            |
            |@Config
            |public interface TransientAbstractConfigDTO {
            |    @ConfigTransient
            |    String invalidAbstractTransient();
            |
            |    int validProp();
            |}
            |""".stripMargin
        )
      )

    assertCompilation(compilation).failed()
    assertCompilation(compilation)
      .hadErrorContaining(
        "Method 'invalidAbstractTransient' in @Config interface 'TransientAbstractConfigDTO' is annotated with @ConfigTransient but is not a default method"
      )
  }
}
