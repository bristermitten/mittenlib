package me.bristermitten.mittenlib.annotations.compile

import com.google.testing.compile.Compilation
import com.google.testing.compile.CompilationSubject.assertThat as assertCompilation
import com.google.testing.compile.Compiler.javac
import com.google.testing.compile.JavaFileObjects
import me.bristermitten.mittenlib.annotations.config.ConfigProcessor
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class ValidatorGeneratorTest extends AnyFunSuite with Matchers {

  test("testCompilationWithAllValidators") {
    val compilation: Compilation = javac()
      .withProcessors(new ConfigProcessor())
      .compile(
        JavaFileObjects.forSourceString(
          "me.bristermitten.mittenlib.tests.MyCustomValidator",
          """
            |package me.bristermitten.mittenlib.tests;
            |
            |import java.util.Optional;
            |import me.bristermitten.mittenlib.config.validation.Validator;
            |
            |public class MyCustomValidator implements Validator<String> {
            |    @Override
            |    public Optional<String> validate(String value) {
            |        if ("invalid".equals(value)) {
            |            return Optional.of("Value cannot be 'invalid'");
            |        }
            |        return Optional.empty();
            |    }
            |}
            |""".stripMargin
        ),
        JavaFileObjects.forSourceString(
          "me.bristermitten.mittenlib.tests.ValidatorTestConfigDTO",
          """
            |package me.bristermitten.mittenlib.tests;
            |
            |import me.bristermitten.mittenlib.config.Config;
            |import me.bristermitten.mittenlib.config.validation.*;
            |import org.jspecify.annotations.Nullable;
            |import java.util.List;
            |import java.util.Map;
            |import java.util.Set;
            |
            |@Config
            |public class ValidatorTestConfigDTO {
            |    @Positive public int positiveInt;
            |    @Negative public double negativeDouble;
            |    @Min(10) public int minInt;
            |    @Max(100) public long maxLong;
            |    @Range(min = 1.0, max = 5.0) public double rangeDouble;
            |    @NotBlank public String notBlankString;
            |    @ValidateWith(MyCustomValidator.class) public String customValidated;
            |
            |    // Collection validations
            |    public List<@NotBlank String> names;
            |    public Set<@Positive Integer> values;
            |    public Map<@NotBlank String, @Min(0) Integer> scores;
            |
            |    // Nullable elements inside collection
            |    public List<@Nullable @NotBlank String> nullableNames;
            |
            |    // Custom validator on collection/map key & value
            |    public List<@ValidateWith(MyCustomValidator.class) String> customList;
            |    public Map<@ValidateWith(MyCustomValidator.class) String, @ValidateWith(MyCustomValidator.class) String> customMap;
            |}
            |""".stripMargin
        )
      )

    assertCompilation(compilation).succeeded()
    assertCompilation(compilation)
      .generatedSourceFile(
        "me.bristermitten.mittenlib.tests.ValidatorTestConfigValidator"
      )
  }
}
