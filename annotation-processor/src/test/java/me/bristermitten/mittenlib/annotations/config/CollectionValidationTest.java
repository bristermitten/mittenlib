package me.bristermitten.mittenlib.annotations.config;

import io.toolisticon.cute.Cute;
import org.junit.jupiter.api.Test;

class CollectionValidationTest {

    @Test
    void testConstraintOnListFailsCompilation() {
        Cute.blackBoxTest()
                .given()
                .processor(ConfigProcessor.class)
                .andSourceFile("ListValidationConfig", """
                        package me.bristermitten.mittenlib.tests;

                        import me.bristermitten.mittenlib.config.Config;
                        import me.bristermitten.mittenlib.config.validation.NotBlank;
                        import java.util.List;

                        @Config
                        public interface ListValidationConfig {
                            @NotBlank List<String> playerNames();
                        }
                        """)
                .whenCompiled()
                .thenExpectThat()
                .compilationFails()
                .andThat()
                .compilerMessage()
                .ofKindError()
                .contains(
                        "Constraint annotation @NotBlank cannot be applied to type java.util.List<java.lang.String>. Expected a String or CharSequence.")
                .executeTest();
    }

    @Test
    void testConstraintOnMapFailsCompilation() {
        Cute.blackBoxTest()
                .given()
                .processor(ConfigProcessor.class)
                .andSourceFile("MapValidationConfig", """
                        package me.bristermitten.mittenlib.tests;

                        import me.bristermitten.mittenlib.config.Config;
                        import me.bristermitten.mittenlib.config.validation.Range;
                        import java.util.Map;

                        @Config
                        public interface MapValidationConfig {
                            @Range(min = 1, max = 100)
                            Map<String, Integer> powerLevels();
                        }
                        """)
                .whenCompiled()
                .thenExpectThat()
                .compilationFails()
                .andThat()
                .compilerMessage()
                .ofKindError()
                .contains(
                        "Constraint annotation @Range cannot be applied to type java.util.Map<java.lang.String, java.lang.Integer>. Expected a numeric type.")
                .executeTest();
    }

    @Test
    void testConstraintOnListElementSucceedsCompilation() {
        Cute.blackBoxTest()
                .given()
                .processor(ConfigProcessor.class)
                .andSourceFile("ListElementValidationConfig", """
                        package me.bristermitten.mittenlib.tests;

                        import me.bristermitten.mittenlib.config.Config;
                        import me.bristermitten.mittenlib.config.validation.NotBlank;
                        import java.util.List;

                        @Config
                        public interface ListElementValidationConfig {
                            List<@NotBlank String> playerNames();
                        }
                        """)
                .whenCompiled()
                .thenExpectThat()
                .compilationSucceeds()
                .andThat()
                .generatedClass("me.bristermitten.mittenlib.tests.ListElementValidationConfigValidator")
                .exists()
                .executeTest();
    }

    @Test
    void testConstraintOnSetElementSucceedsCompilation() {
        Cute.blackBoxTest()
                .given()
                .processor(ConfigProcessor.class)
                .andSourceFile("SetElementValidationConfig", """
                        package me.bristermitten.mittenlib.tests;

                        import me.bristermitten.mittenlib.config.Config;
                        import me.bristermitten.mittenlib.config.validation.NotBlank;
                        import java.util.Set;

                        @Config
                        public interface SetElementValidationConfig {
                            Set<@NotBlank String> playerNames();
                        }
                        """)
                .whenCompiled()
                .thenExpectThat()
                .compilationSucceeds()
                .andThat()
                .generatedClass("me.bristermitten.mittenlib.tests.SetElementValidationConfigValidator")
                .exists()
                .executeTest();
    }

    @Test
    void testConstraintOnMapKeyAndValueSucceedsCompilation() {
        Cute.blackBoxTest()
                .given()
                .processor(ConfigProcessor.class)
                .andSourceFile("MapKeyAndValueValidationConfig", """
                        package me.bristermitten.mittenlib.tests;

                        import me.bristermitten.mittenlib.config.Config;
                        import me.bristermitten.mittenlib.config.validation.NotBlank;
                        import me.bristermitten.mittenlib.config.validation.Min;
                        import java.util.Map;

                        @Config
                        public interface MapKeyAndValueValidationConfig {
                            Map<@NotBlank String, @Min(0) Integer> powerLevels();
                        }
                        """)
                .whenCompiled()
                .thenExpectThat()
                .compilationSucceeds()
                .andThat()
                .generatedClass("me.bristermitten.mittenlib.tests.MapKeyAndValueValidationConfigValidator")
                .exists()
                .executeTest();
    }

    @Test
    void testConstraintOnListElementMismatchFailsCompilation() {
        Cute.blackBoxTest()
                .given()
                .processor(ConfigProcessor.class)
                .andSourceFile("ListElementMismatchValidationConfig", """
                        package me.bristermitten.mittenlib.tests;

                        import me.bristermitten.mittenlib.config.Config;
                        import me.bristermitten.mittenlib.config.validation.NotBlank;
                        import java.util.List;

                        @Config
                        public interface ListElementMismatchValidationConfig {
                            List<@NotBlank Integer> playerLevels();
                        }
                        """)
                .whenCompiled()
                .thenExpectThat()
                .compilationFails()
                .andThat()
                .compilerMessage()
                .ofKindError()
                .contains(
                        "Constraint annotation @NotBlank cannot be applied to type java.lang.Integer. Expected a String or CharSequence.")
                .executeTest();
    }

    @Test
    void testConstraintOnListElementCustomValidatorSucceedsCompilation() {
        Cute.blackBoxTest()
                .given()
                .processor(ConfigProcessor.class)
                .andSourceFile("ListElementCustomValidatorConfig", """
                        package me.bristermitten.mittenlib.tests;

                        import me.bristermitten.mittenlib.config.Config;
                        import me.bristermitten.mittenlib.config.validation.ValidateWith;
                        import java.util.List;

                        @Config
                        public interface ListElementCustomValidatorConfig {
                            List<@ValidateWith(DummyValidator.class) String> names();
                        }
                        """)
                .andSourceFile("DummyValidator", """
                        package me.bristermitten.mittenlib.tests;

                        import me.bristermitten.mittenlib.config.validation.Validator;
                        import java.util.Optional;

                        public class DummyValidator implements Validator<String> {
                            @Override
                            public Optional<String> validate(String value) {
                                return Optional.empty();
                            }
                        }
                        """)
                .whenCompiled()
                .thenExpectThat()
                .compilationSucceeds()
                .andThat()
                .generatedClass("me.bristermitten.mittenlib.tests.ListElementCustomValidatorConfigValidator")
                .exists()
                .executeTest();
    }

    @Test
    void testConstraintOnMapKeyCustomValidatorSucceedsCompilation() {
        Cute.blackBoxTest()
                .given()
                .processor(ConfigProcessor.class)
                .andSourceFile("MapKeyCustomValidatorConfig", """
                        package me.bristermitten.mittenlib.tests;

                        import me.bristermitten.mittenlib.config.Config;
                        import me.bristermitten.mittenlib.config.validation.ValidateWith;
                        import java.util.Map;

                        @Config
                        public interface MapKeyCustomValidatorConfig {
                            Map<@ValidateWith(DummyValidator.class) String, Integer> scores();
                        }
                        """)
                .andSourceFile("DummyValidator", """
                        package me.bristermitten.mittenlib.tests;

                        import me.bristermitten.mittenlib.config.validation.Validator;
                        import java.util.Optional;

                        public class DummyValidator implements Validator<String> {
                            @Override
                            public Optional<String> validate(String value) {
                                return Optional.empty();
                            }
                        }
                        """)
                .whenCompiled()
                .thenExpectThat()
                .compilationSucceeds()
                .andThat()
                .generatedClass("me.bristermitten.mittenlib.tests.MapKeyCustomValidatorConfigValidator")
                .exists()
                .executeTest();
    }

    @Test
    void testConstraintOnMapValueMismatchFailsCompilation() {
        Cute.blackBoxTest()
                .given()
                .processor(ConfigProcessor.class)
                .andSourceFile("MapValueMismatchValidationConfig", """
                        package me.bristermitten.mittenlib.tests;

                        import me.bristermitten.mittenlib.config.Config;
                        import me.bristermitten.mittenlib.config.validation.NotBlank;
                        import java.util.Map;

                        @Config
                        public interface MapValueMismatchValidationConfig {
                            Map<String, @NotBlank Integer> playerLevels();
                        }
                        """)
                .whenCompiled()
                .thenExpectThat()
                .compilationFails()
                .andThat()
                .compilerMessage()
                .ofKindError()
                .contains(
                        "Constraint annotation @NotBlank cannot be applied to type java.lang.Integer. Expected a String or CharSequence.")
                .executeTest();
    }
}
