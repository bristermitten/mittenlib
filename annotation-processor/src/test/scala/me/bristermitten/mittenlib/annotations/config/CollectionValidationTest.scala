package me.bristermitten.mittenlib.annotations.config

import io.toolisticon.cute.Cute
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class CollectionValidationTest extends AnyFunSuite with Matchers {

  test("testConstraintOnListFailsCompilation") {
    Cute
      .blackBoxTest()
      .`given`()
      .processor(classOf[ConfigProcessor])
      .andSourceFile(
        "ListValidationConfig",
        """
          |package me.bristermitten.mittenlib.tests;
          |
          |import me.bristermitten.mittenlib.config.Config;
          |import me.bristermitten.mittenlib.config.validation.NotBlank;
          |import java.util.List;
          |
          |@Config
          |public interface ListValidationConfig {
          |    @NotBlank List<String> playerNames();
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
        "Constraint annotation @NotBlank cannot be applied to type java.util.List<java.lang.String>. Expected a String or CharSequence."
      )
      .executeTest()
  }

  test("testConstraintOnMapFailsCompilation") {
    Cute
      .blackBoxTest()
      .`given`()
      .processor(classOf[ConfigProcessor])
      .andSourceFile(
        "MapValidationConfig",
        """
          |package me.bristermitten.mittenlib.tests;
          |
          |import me.bristermitten.mittenlib.config.Config;
          |import me.bristermitten.mittenlib.config.validation.Range;
          |import java.util.Map;
          |
          |@Config
          |public interface MapValidationConfig {
          |    @Range(min = 1, max = 100)
          |    Map<String, Integer> powerLevels();
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
        "Constraint annotation @Range cannot be applied to type java.util.Map<java.lang.String, java.lang.Integer>. Expected a numeric type."
      )
      .executeTest()
  }

  test("testConstraintOnListElementSucceedsCompilation") {
    Cute
      .blackBoxTest()
      .`given`()
      .processor(classOf[ConfigProcessor])
      .andSourceFile(
        "ListElementValidationConfig",
        """
          |package me.bristermitten.mittenlib.tests;
          |
          |import me.bristermitten.mittenlib.config.Config;
          |import me.bristermitten.mittenlib.config.validation.NotBlank;
          |import java.util.List;
          |
          |@Config
          |public interface ListElementValidationConfig {
          |    List<@NotBlank String> playerNames();
          |}
          |""".stripMargin
      )
      .whenCompiled()
      .thenExpectThat()
      .compilationSucceeds()
      .andThat()
      .generatedClass(
        "me.bristermitten.mittenlib.tests.ListElementValidationConfigValidator"
      )
      .exists()
      .executeTest()
  }

  test("testConstraintOnSetElementSucceedsCompilation") {
    Cute
      .blackBoxTest()
      .`given`()
      .processor(classOf[ConfigProcessor])
      .andSourceFile(
        "SetElementValidationConfig",
        """
          |package me.bristermitten.mittenlib.tests;
          |
          |import me.bristermitten.mittenlib.config.Config;
          |import me.bristermitten.mittenlib.config.validation.NotBlank;
          |import java.util.Set;
          |
          |@Config
          |public interface SetElementValidationConfig {
          |    Set<@NotBlank String> playerNames();
          |}
          |""".stripMargin
      )
      .whenCompiled()
      .thenExpectThat()
      .compilationSucceeds()
      .andThat()
      .generatedClass(
        "me.bristermitten.mittenlib.tests.SetElementValidationConfigValidator"
      )
      .exists()
      .executeTest()
  }

  test("testConstraintOnMapKeyAndValueSucceedsCompilation") {
    Cute
      .blackBoxTest()
      .`given`()
      .processor(classOf[ConfigProcessor])
      .andSourceFile(
        "MapKeyAndValueValidationConfig",
        """
          |package me.bristermitten.mittenlib.tests;
          |
          |import me.bristermitten.mittenlib.config.Config;
          |import me.bristermitten.mittenlib.config.validation.NotBlank;
          |import me.bristermitten.mittenlib.config.validation.Min;
          |import java.util.Map;
          |
          |@Config
          |public interface MapKeyAndValueValidationConfig {
          |    Map<@NotBlank String, @Min(0) Integer> powerLevels();
          |}
          |""".stripMargin
      )
      .whenCompiled()
      .thenExpectThat()
      .compilationSucceeds()
      .andThat()
      .generatedClass(
        "me.bristermitten.mittenlib.tests.MapKeyAndValueValidationConfigValidator"
      )
      .exists()
      .executeTest()
  }

  test("testConstraintOnListElementMismatchFailsCompilation") {
    Cute
      .blackBoxTest()
      .`given`()
      .processor(classOf[ConfigProcessor])
      .andSourceFile(
        "ListElementMismatchValidationConfig",
        """
          |package me.bristermitten.mittenlib.tests;
          |
          |import me.bristermitten.mittenlib.config.Config;
          |import me.bristermitten.mittenlib.config.validation.NotBlank;
          |import java.util.List;
          |
          |@Config
          |public interface ListElementMismatchValidationConfig {
          |    List<@NotBlank Integer> playerLevels();
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
        "Constraint annotation @NotBlank cannot be applied to type java.lang.Integer. Expected a String or CharSequence."
      )
      .executeTest()
  }

  test("testConstraintOnListElementCustomValidatorSucceedsCompilation") {
    Cute
      .blackBoxTest()
      .`given`()
      .processor(classOf[ConfigProcessor])
      .andSourceFile(
        "ListElementCustomValidatorConfig",
        """
          |package me.bristermitten.mittenlib.tests;
          |
          |import me.bristermitten.mittenlib.config.Config;
          |import me.bristermitten.mittenlib.config.validation.ValidateWith;
          |import java.util.List;
          |
          |@Config
          |public interface ListElementCustomValidatorConfig {
          |    List<@ValidateWith(DummyValidator.class) String> names();
          |}
          |""".stripMargin
      )
      .andSourceFile(
        "DummyValidator",
        """
          |package me.bristermitten.mittenlib.tests;
          |
          |import me.bristermitten.mittenlib.config.validation.Validator;
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
      .compilationSucceeds()
      .andThat()
      .generatedClass(
        "me.bristermitten.mittenlib.tests.ListElementCustomValidatorConfigValidator"
      )
      .exists()
      .executeTest()
  }

  test("testConstraintOnMapKeyCustomValidatorSucceedsCompilation") {
    Cute
      .blackBoxTest()
      .`given`()
      .processor(classOf[ConfigProcessor])
      .andSourceFile(
        "MapKeyCustomValidatorConfig",
        """
          |package me.bristermitten.mittenlib.tests;
          |
          |import me.bristermitten.mittenlib.config.Config;
          |import me.bristermitten.mittenlib.config.validation.ValidateWith;
          |import java.util.Map;
          |
          |@Config
          |public interface MapKeyCustomValidatorConfig {
          |    Map<@ValidateWith(DummyValidator.class) String, Integer> scores();
          |}
          |""".stripMargin
      )
      .andSourceFile(
        "DummyValidator",
        """
          |package me.bristermitten.mittenlib.tests;
          |
          |import me.bristermitten.mittenlib.config.validation.Validator;
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
      .compilationSucceeds()
      .andThat()
      .generatedClass(
        "me.bristermitten.mittenlib.tests.MapKeyCustomValidatorConfigValidator"
      )
      .exists()
      .executeTest()
  }

  test("testConstraintOnMapValueMismatchFailsCompilation") {
    Cute
      .blackBoxTest()
      .`given`()
      .processor(classOf[ConfigProcessor])
      .andSourceFile(
        "MapValueMismatchValidationConfig",
        """
          |package me.bristermitten.mittenlib.tests;
          |
          |import me.bristermitten.mittenlib.config.Config;
          |import me.bristermitten.mittenlib.config.validation.NotBlank;
          |import java.util.Map;
          |
          |@Config
          |public interface MapValueMismatchValidationConfig {
          |    Map<String, @NotBlank Integer> playerLevels();
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
        "Constraint annotation @NotBlank cannot be applied to type java.lang.Integer. Expected a String or CharSequence."
      )
      .executeTest()
  }
}
