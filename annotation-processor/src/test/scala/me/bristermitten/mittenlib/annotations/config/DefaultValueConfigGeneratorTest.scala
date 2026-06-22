package me.bristermitten.mittenlib.annotations.config

import com.google.testing.compile.Compilation
import com.google.testing.compile.CompilationSubject.assertThat as assertCompilation
import com.google.testing.compile.Compiler.javac
import com.google.testing.compile.JavaFileObjects
import io.toolisticon.cute.Cute
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class DefaultValueConfigGeneratorTest extends AnyFunSuite with Matchers {

  test("generateFullConfigClassName") {
    val compilation: Compilation = javac()
      .withProcessors(new ConfigProcessor())
      .compile(
        JavaFileObjects.forSourceString(
          "me.bristermitten.mittenlib.tests.DefaultValueConfigDTO",
          """
          |package me.bristermitten.mittenlib.tests;
          |
          |import me.bristermitten.mittenlib.config.Config;
          |
          |@Config
          |public class DefaultValueConfigDTO {
          |    int x = 3;
          |    int y;
          |    Integer z = null;
          |}
          |""".stripMargin
        )
      )

    assertCompilation(compilation).succeeded()
    assertCompilation(compilation)
      .generatedSourceFile(
        "me.bristermitten.mittenlib.tests.DefaultValueConfig"
      )
      .isNotNull()
  }

  test("testWithInterface") {
    Cute
      .blackBoxTest()
      .`given`()
      .processor(classOf[ConfigProcessor])
      .andSourceFile(
        "DefaultValueConfig",
        """
          |package me.bristermitten.mittenlib.tests;
          |
          |import me.bristermitten.mittenlib.config.Config;
          |
          |@Config
          |public interface DefaultValueConfig {
          |    default int x() {
          |        return 3;
          |    }
          |    int y();
          |    default Integer z() {
          |        return null;
          |    }
          |}
          |""".stripMargin
      )
      .whenCompiled()
      .thenExpectThat()
      .compilationSucceeds()
      .andThat()
      .generatedClass("me.bristermitten.mittenlib.tests.DefaultValueConfigImpl")
      .exists()
      .executeTest()
  }

  test("testWithInterfaceAllDefault") {
    Cute
      .blackBoxTest()
      .`given`()
      .processor(classOf[ConfigProcessor])
      .andSourceFile(
        "DefaultValueConfigAllDefault",
        """
          |package me.bristermitten.mittenlib.tests;
          |
          |import me.bristermitten.mittenlib.config.Config;
          |
          |@Config
          |public interface DefaultValueConfigAllDefault {
          |    default int x() {
          |        return 3;
          |    }
          |    default Integer z() {
          |        return null;
          |    }
          |}
          |""".stripMargin
      )
      .whenCompiled()
      .thenExpectThat()
      .compilationSucceeds()
      .andThat()
      .generatedClass(
        "me.bristermitten.mittenlib.tests.DefaultValueConfigAllDefaultImpl"
      )
      .exists()
      .executeTest()
  }

  test("testClassDTOWithoutNoArgConstructorAndNoDefaults") {
    Cute
      .blackBoxTest()
      .`given`()
      .processor(classOf[ConfigProcessor])
      .andSourceFile(
        "NoArgConstructorDTO",
        """
          |package me.bristermitten.mittenlib.tests;
          |
          |import me.bristermitten.mittenlib.config.Config;
          |
          |@Config
          |public class NoArgConstructorDTO {
          |    public int x;
          |
          |    public NoArgConstructorDTO(int x) {
          |        this.x = x;
          |    }
          |}
          |""".stripMargin
      )
      .whenCompiled()
      .thenExpectThat()
      .compilationSucceeds()
      .andThat()
      .generatedClass("me.bristermitten.mittenlib.tests.NoArgConstructor")
      .exists()
      .executeTest()
  }

  test("testClassDTOWithNonPublicConstructor") {
    Cute
      .blackBoxTest()
      .`given`()
      .processor(classOf[ConfigProcessor])
      .andSourceFile(
        "NonPublicConstructorDTO",
        """
          |package me.bristermitten.mittenlib.tests;
          |
          |import me.bristermitten.mittenlib.config.Config;
          |
          |@Config
          |public class NonPublicConstructorDTO {
          |    public int x;
          |
          |    NonPublicConstructorDTO(int x) {
          |        this.x = x;
          |    }
          |}
          |""".stripMargin
      )
      .whenCompiled()
      .thenExpectThat()
      .compilationSucceeds()
      .andThat()
      .generatedClass("me.bristermitten.mittenlib.tests.NonPublicConstructor")
      .exists()
      .executeTest()
  }

  test("testClassDTOWithConstructorAndDefaultValueAndNoArgConstructor") {
    Cute
      .blackBoxTest()
      .`given`()
      .processor(classOf[ConfigProcessor])
      .andSourceFile(
        "ConstructorAndDefaultValueDTO",
        """
          |package me.bristermitten.mittenlib.tests;
          |
          |import me.bristermitten.mittenlib.config.Config;
          |
          |@Config
          |public class ConstructorAndDefaultValueDTO {
          |    public int x = 3;
          |    public int y;
          |
          |    ConstructorAndDefaultValueDTO() {}
          |
          |    public ConstructorAndDefaultValueDTO(int y) {
          |        this.y = y;
          |    }
          |}
          |""".stripMargin
      )
      .whenCompiled()
      .thenExpectThat()
      .compilationSucceeds()
      .andThat()
      .generatedClass(
        "me.bristermitten.mittenlib.tests.ConstructorAndDefaultValue"
      )
      .exists()
      .executeTest()
  }

  test("testClassDTOWithConstructorAndDefaultValueAndNoNoArgConstructorFails") {
    Cute
      .blackBoxTest()
      .`given`()
      .processor(classOf[ConfigProcessor])
      .andSourceFile(
        "ConstructorAndDefaultValueNoNoArgDTO",
        """
          |package me.bristermitten.mittenlib.tests;
          |
          |import me.bristermitten.mittenlib.config.Config;
          |
          |@Config
          |public class ConstructorAndDefaultValueNoNoArgDTO {
          |    public int x = 3;
          |    public int y;
          |
          |    public ConstructorAndDefaultValueNoNoArgDTO(int y) {
          |        this.y = y;
          |    }
          |}
          |""".stripMargin
      )
      .whenCompiled()
      .thenExpectThat()
      .compilationFails()
      .andThat()
      .compilerMessage()
      .ofKindError()
      .contains(
        "has fields with default values, but is missing an accessible (non-private) zero-arguments constructor"
      )
      .executeTest()
  }

  test("testClassDTOWithPrivateNoArgConstructorFails") {
    Cute
      .blackBoxTest()
      .`given`()
      .processor(classOf[ConfigProcessor])
      .andSourceFile(
        "PrivateNoArgDTO",
        """
          |package me.bristermitten.mittenlib.tests;
          |
          |import me.bristermitten.mittenlib.config.Config;
          |
          |@Config
          |public class PrivateNoArgDTO {
          |    public int x = 3;
          |
          |    private PrivateNoArgDTO() {}
          |}
          |""".stripMargin
      )
      .whenCompiled()
      .thenExpectThat()
      .compilationFails()
      .andThat()
      .compilerMessage()
      .ofKindError()
      .contains(
        "has fields with default values, but is missing an accessible (non-private) zero-arguments constructor"
      )
      .executeTest()
  }

  test("testConstraintTypeMismatchNumeric") {
    Cute
      .blackBoxTest()
      .`given`()
      .processor(classOf[ConfigProcessor])
      .andSourceFile(
        "InvalidNumericConstraintDTO",
        """
          |package me.bristermitten.mittenlib.tests;
          |
          |import me.bristermitten.mittenlib.config.Config;
          |import me.bristermitten.mittenlib.config.validation.Positive;
          |
          |@Config
          |public class InvalidNumericConstraintDTO {
          |    @Positive public String name;
          |}
          |""".stripMargin
      )
      .whenCompiled()
      .thenExpectThat()
      .compilationFails()
      .andThat()
      .compilerMessage()
      .ofKindError()
      .contains(
        "Constraint annotation @Positive cannot be applied to type java.lang.String. Expected a numeric type."
      )
      .executeTest()
  }

  test("testConstraintTypeMismatchString") {
    Cute
      .blackBoxTest()
      .`given`()
      .processor(classOf[ConfigProcessor])
      .andSourceFile(
        "InvalidStringConstraintDTO",
        """
          |package me.bristermitten.mittenlib.tests;
          |
          |import me.bristermitten.mittenlib.config.Config;
          |import me.bristermitten.mittenlib.config.validation.NotBlank;
          |
          |@Config
          |public class InvalidStringConstraintDTO {
          |    @NotBlank public int age;
          |}
          |""".stripMargin
      )
      .whenCompiled()
      .thenExpectThat()
      .compilationFails()
      .andThat()
      .compilerMessage()
      .ofKindError()
      .contains(
        "Constraint annotation @NotBlank cannot be applied to type int. Expected a String or CharSequence."
      )
      .executeTest()
  }

  test("testConstraintTypeMismatchValidateWith") {
    Cute
      .blackBoxTest()
      .`given`()
      .processor(classOf[ConfigProcessor])
      .andSourceFile(
        "InvalidValidateWithDTO",
        """
          |package me.bristermitten.mittenlib.tests;
          |
          |import me.bristermitten.mittenlib.config.Config;
          |import me.bristermitten.mittenlib.config.validation.ValidateWith;
          |
          |@Config
          |public class InvalidValidateWithDTO {
          |    @ValidateWith(DummyValidator.class)
          |    public int age;
          |}
          |""".stripMargin
      )
      .andSourceFile(
        "DummyValidator",
        """
          |package me.bristermitten.mittenlib.tests;
          |
          |import me.bristermitten.mittenlib.config.validation.Validator;
          |
          |import java.util.Optional;
          |
          |public class DummyValidator implements Validator<String> {
          |    @Override
          |    public Optional<String> validate(String value) {
          |        return Optional.empty();
          |    }
          |}
          |""".stripMargin
      )
      .whenCompiled()
      .thenExpectThat()
      .compilationFails()
      .andThat()
      .compilerMessage()
      .ofKindError()
      .contains(
        "Constraint annotation @ValidateWith(DummyValidator.class) cannot be applied to type int. Expected a Validator compatible with int."
      )
      .executeTest()
  }
}
