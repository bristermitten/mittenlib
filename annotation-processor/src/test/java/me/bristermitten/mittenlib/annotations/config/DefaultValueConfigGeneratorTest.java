package me.bristermitten.mittenlib.annotations.config;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import io.toolisticon.cute.Cute;
import org.junit.jupiter.api.Test;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class DefaultValueConfigGeneratorTest {

    @Test
    void generateFullConfigClassName() {
        Compilation compilation = javac()
                .withProcessors(new ConfigProcessor())
                .compile(JavaFileObjects.forSourceString("me.bristermitten.mittenlib.tests.DefaultValueConfigDTO",
                        """
                                package me.bristermitten.mittenlib.tests;
                                
                                import me.bristermitten.mittenlib.config.Config;
                                
                                @Config
                                public class DefaultValueConfigDTO {
                                    int x = 3;
                                    int y;
                                    Integer z = null;
                                }
                                """));

        assertThat(compilation).succeededWithoutWarnings();
        assertThat(compilation).generatedSourceFile("me.bristermitten.mittenlib.tests.DefaultValueConfig")
                .isNotNull();
    }

    @Test
    void testWithInterface() {
        Cute.blackBoxTest().given().processor(ConfigProcessor.class)
                .andSourceFile("DefaultValueConfig", """
                        package me.bristermitten.mittenlib.tests;
                        
                        import me.bristermitten.mittenlib.config.Config;
                        
                        @Config
                        public interface DefaultValueConfig {
                            default int x() {
                                return 3;
                            }
                            int y();
                            default Integer z() {
                                return null;
                            }
                        }
                        """)
                .whenCompiled()
                .thenExpectThat().compilationSucceeds()
                .andThat()
                .generatedClass("me.bristermitten.mittenlib.tests.DefaultValueConfigImpl")
                .exists()
                .executeTest();
    }

    @Test
    void testWithInterfaceAllDefault() {
        Cute.blackBoxTest().given().processor(ConfigProcessor.class)
                .andSourceFile("DefaultValueConfigAllDefault", """
                        package me.bristermitten.mittenlib.tests;
                        
                        import me.bristermitten.mittenlib.config.Config;
                        
                        @Config
                        public interface DefaultValueConfigAllDefault {
                            default int x() {
                                return 3;
                            }
                            default Integer z() {
                                return null;
                            }
                        }
                        """)
                .whenCompiled()
                .thenExpectThat().compilationSucceeds()
                .andThat()
                .generatedClass("me.bristermitten.mittenlib.tests.DefaultValueConfigAllDefaultImpl")
                .exists()
                .executeTest();
    }

    @Test
    void testClassDTOWithoutNoArgConstructorAndNoDefaults() {
        Cute.blackBoxTest().given().processor(ConfigProcessor.class)
                .andSourceFile("NoArgConstructorDTO",
                        """
                                package me.bristermitten.mittenlib.tests;
                                
                                import me.bristermitten.mittenlib.config.Config;
                                
                                @Config
                                public class NoArgConstructorDTO {
                                    public int x;
                                
                                    public NoArgConstructorDTO(int x) {
                                        this.x = x;
                                    }
                                }
                                """)
                .whenCompiled()
                .thenExpectThat().compilationSucceeds()
                .andThat()
                .generatedClass("me.bristermitten.mittenlib.tests.NoArgConstructor")
                .exists()
                .executeTest();
    }

    @Test
    void testClassDTOWithNonPublicConstructor() {
        Cute.blackBoxTest().given().processor(ConfigProcessor.class)
                .andSourceFile("NonPublicConstructorDTO", """
                        package me.bristermitten.mittenlib.tests;
                        
                        import me.bristermitten.mittenlib.config.Config;
                        
                        @Config
                        public class NonPublicConstructorDTO {
                            public int x;
                        
                            NonPublicConstructorDTO(int x) {
                                this.x = x;
                            }
                        }
                        """)
                .whenCompiled()
                .thenExpectThat().compilationSucceeds()
                .andThat()
                .generatedClass("me.bristermitten.mittenlib.tests.NonPublicConstructor")
                .exists()
                .executeTest();
    }

    @Test
    void testClassDTOWithConstructorAndDefaultValueAndNoArgConstructor() {
        Cute.blackBoxTest().given().processor(ConfigProcessor.class)
                .andSourceFile("ConstructorAndDefaultValueDTO", """
                        package me.bristermitten.mittenlib.tests;
                        
                        import me.bristermitten.mittenlib.config.Config;
                        
                        @Config
                        public class ConstructorAndDefaultValueDTO {
                            public int x = 3;
                            public int y;
                        
                            ConstructorAndDefaultValueDTO() {}
                        
                            public ConstructorAndDefaultValueDTO(int y) {
                                this.y = y;
                            }
                        }
                        """)
                .whenCompiled()
                .thenExpectThat().compilationSucceeds()
                .andThat()
                .generatedClass("me.bristermitten.mittenlib.tests.ConstructorAndDefaultValue")
                .exists()
                .executeTest();
    }

    @Test
    void testClassDTOWithConstructorAndDefaultValueAndNoNoArgConstructorFails() {
        Cute.blackBoxTest().given().processor(ConfigProcessor.class)
                .andSourceFile("ConstructorAndDefaultValueNoNoArgDTO", """
                        package me.bristermitten.mittenlib.tests;
                        
                        import me.bristermitten.mittenlib.config.Config;
                        
                        @Config
                        public class ConstructorAndDefaultValueNoNoArgDTO {
                            public int x = 3;
                            public int y;
                        
                            public ConstructorAndDefaultValueNoNoArgDTO(int y) {
                                this.y = y;
                            }
                        }
                        """)
                .whenCompiled()
                .thenExpectThat().compilationFails()
                .andThat()
                .compilerMessage().ofKindError().contains("has fields with default values, but is missing a zero-arguments constructor")
                .executeTest();
    }

    @Test
    void testConstraintTypeMismatchNumeric() {
        Cute.blackBoxTest().given().processor(ConfigProcessor.class)
                .andSourceFile("InvalidNumericConstraintDTO", """
                        package me.bristermitten.mittenlib.tests;
                        
                        import me.bristermitten.mittenlib.config.Config;
                        import me.bristermitten.mittenlib.config.validation.Positive;
                        
                        @Config
                        public class InvalidNumericConstraintDTO {
                            @Positive
                            public String name;
                        }
                        """)
                .whenCompiled()
                .thenExpectThat().compilationFails()
                .andThat()
                .compilerMessage().ofKindError().contains("Constraint annotation @Positive cannot be applied to type java.lang.String. Expected a numeric type.")
                .executeTest();
    }

    @Test
    void testConstraintTypeMismatchString() {
        Cute.blackBoxTest().given().processor(ConfigProcessor.class)
                .andSourceFile("InvalidStringConstraintDTO", """
                        package me.bristermitten.mittenlib.tests;
                        
                        import me.bristermitten.mittenlib.config.Config;
                        import me.bristermitten.mittenlib.config.validation.NotBlank;
                        
                        @Config
                        public class InvalidStringConstraintDTO {
                            @NotBlank
                            public int age;
                        }
                        """)
                .whenCompiled()
                .thenExpectThat().compilationFails()
                .andThat()
                .compilerMessage().ofKindError().contains("Constraint annotation @NotBlank cannot be applied to type int. Expected a String or CharSequence.")
                .executeTest();
    }

    @Test
    void testConstraintTypeMismatchValidateWith() {
        Cute.blackBoxTest().given().processor(ConfigProcessor.class)
                .andSourceFile("InvalidValidateWithDTO", """
                        package me.bristermitten.mittenlib.tests;
                        
                        import me.bristermitten.mittenlib.config.Config;
                        import me.bristermitten.mittenlib.config.validation.ValidateWith;
                        
                        @Config
                        public class InvalidValidateWithDTO {
                            @ValidateWith(DummyValidator.class)
                            public int age;
                        }
                        """)
                .andSourceFile("DummyValidator", """
                        package me.bristermitten.mittenlib.tests;
                        
                        import me.bristermitten.mittenlib.config.validation.Validator;
                        
                        public class DummyValidator implements Validator<String> {
                            @Override
                            public java.util.Optional<String> validate(String value) {
                                return java.util.Optional.empty();
                            }
                        }
                        """)
                .whenCompiled()
                .thenExpectThat().compilationFails()
                .andThat()
                .compilerMessage().ofKindError().contains("Constraint annotation @ValidateWith(DummyValidator.class) cannot be applied to type int. Expected a Validator compatible with int.")
                .executeTest();
    }
}
