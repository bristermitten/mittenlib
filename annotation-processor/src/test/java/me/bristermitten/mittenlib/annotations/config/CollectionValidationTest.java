package me.bristermitten.mittenlib.annotations.config;

import io.toolisticon.cute.Cute;
import org.junit.jupiter.api.Test;

class CollectionValidationTest {

    @Test
    void testConstraintOnListFailsCompilation() {
        Cute.blackBoxTest()
                .given()
                .processor(ConfigProcessor.class)
                .andSourceFile(
                        "ListValidationConfig",
                        """
                        package me.bristermitten.mittenlib.tests;

                        import me.bristermitten.mittenlib.config.Config;
                        import me.bristermitten.mittenlib.config.validation.NotBlank;
                        import java.util.List;

                        @Config
                        public interface ListValidationConfig {
                            @NotBlank
                            List<String> playerNames();
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
                .andSourceFile(
                        "MapValidationConfig",
                        """
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
                        "Constraint annotation @Range cannot be applied to type java.util.Map<java.lang.String,java.lang.Integer>. Expected a numeric type.")
                .executeTest();
    }
}
