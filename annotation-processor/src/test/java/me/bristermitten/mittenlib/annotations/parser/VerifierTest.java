package me.bristermitten.mittenlib.annotations.parser;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import me.bristermitten.mittenlib.annotations.config.ConfigProcessor;
import org.junit.jupiter.api.Test;

class VerifierTest {

    @Test
    void testUnionAlternativeNotExtendingUnion() {
        Compilation compilation = javac().withProcessors(new ConfigProcessor())
                .compile(JavaFileObjects.forSourceString("me.bristermitten.mittenlib.tests.UnionConfigDTO", """
                        package me.bristermitten.mittenlib.tests;
                        import me.bristermitten.mittenlib.config.*;

                        @Config
                        @ConfigUnion
                        public class UnionConfigDTO {
                            public int sharedProperty;

                            @Config
                            public static class AlternativeOneDTO {
                                // Missing 'extends UnionConfigDTO'
                                public String altProperty;
                            }
                        }
                        """));

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("UnionConfigDTO MUST extend the union type");
    }

    @Test
    void testEnumParsingSchemeNotEnumWarning() {
        Compilation compilation = javac().withProcessors(new ConfigProcessor())
                .compile(JavaFileObjects.forSourceString("me.bristermitten.mittenlib.tests.EnumWarnConfigDTO", """
                        package me.bristermitten.mittenlib.tests;
                        import me.bristermitten.mittenlib.config.*;
                        import me.bristermitten.mittenlib.config.names.*;

                        @Config
                        public class EnumWarnConfigDTO {
                            @EnumParsingScheme(EnumParsingSchemes.EXACT_MATCH)
                            public String notAnEnumField;
                        }
                        """));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .hadWarningContaining(
                        "This property's type is not an enum, so the @EnumParsingScheme annotation will have no effect");
    }

    @Test
    void testClassDtoMissingNoArgConstructor() {
        Compilation compilation = javac().withProcessors(new ConfigProcessor())
                .compile(
                        JavaFileObjects.forSourceString("me.bristermitten.mittenlib.tests.NoArgMissingConfigDTO", """
                        package me.bristermitten.mittenlib.tests;
                        import me.bristermitten.mittenlib.config.*;

                        @Config(requireDynamicInitialization = false)
                        public class NoArgMissingConfigDTO {
                            public int level = 5;

                            private NoArgMissingConfigDTO(int level) {
                                this.level = level;
                            }
                        }
                        """));

        assertThat(compilation).failed();
        assertThat(compilation)
                .hadErrorContaining(
                        "has fields with default values, but is missing an accessible (non-private) zero-arguments constructor");
    }

    @Test
    void testConstraintTypeMismatch() {
        Compilation compilation = javac().withProcessors(new ConfigProcessor())
                .compile(JavaFileObjects.forSourceString(
                        "me.bristermitten.mittenlib.tests.ConstraintMismatchConfigDTO", """
                        package me.bristermitten.mittenlib.tests;
                        import me.bristermitten.mittenlib.config.*;
                        import me.bristermitten.mittenlib.config.validation.*;

                        @Config
                        public class ConstraintMismatchConfigDTO {
                            @NotBlank public int invalidNotBlankOnInt;

                            @Positive public String invalidPositiveOnString;
                        }
                        """));

        assertThat(compilation).failed();
        assertThat(compilation)
                .hadErrorContaining(
                        "Constraint annotation @NotBlank cannot be applied to type int. Expected a String or CharSequence");
        assertThat(compilation)
                .hadErrorContaining(
                        "Constraint annotation @Positive cannot be applied to type java.lang.String. Expected a numeric type");
    }

    @Test
    void testSerializationNotSupported() {
        Compilation compilation = javac().withProcessors(new ConfigProcessor())
                .compile(JavaFileObjects.forSourceString(
                        "me.bristermitten.mittenlib.tests.UnserializableConfigDTO", """
                        package me.bristermitten.mittenlib.tests;
                        import me.bristermitten.mittenlib.config.*;

                        @Config(requireSerialization = true)
                        public class UnserializableConfigDTO {
                            public java.lang.Thread unsupportedField;
                        }
                        """));

        assertThat(compilation).failed();
        assertThat(compilation)
                .hadErrorContaining(
                        "Serialization is required for this config, but it contains properties that cannot be serialized");
    }

    @Test
    void testSerializationNotSupportedWarning() {
        Compilation compilation = javac().withProcessors(new ConfigProcessor())
                .compile(JavaFileObjects.forSourceString(
                        "me.bristermitten.mittenlib.tests.UnserializableWarnConfigDTO", """
                        package me.bristermitten.mittenlib.tests;
                        import me.bristermitten.mittenlib.config.*;

                        @Config(requireSerialization = false)
                        public class UnserializableWarnConfigDTO {
                            public java.lang.Thread unsupportedField;
                        }
                        """));

        assertThat(compilation).succeeded();
        assertThat(compilation).hadWarningContaining("This config contains properties that cannot be serialized");
    }

    @Test
    void testNotDynamicallyInitializableError() {
        Compilation compilation = javac().withProcessors(new ConfigProcessor())
                .compile(JavaFileObjects.forSourceString("me.bristermitten.mittenlib.tests.NotDynInitConfigDTO", """
                        package me.bristermitten.mittenlib.tests;
                        import me.bristermitten.mittenlib.config.*;

                        @Source("config.yml")
                        @Config(requireDynamicInitialization = true)
                        public class NotDynInitConfigDTO {
                            public String requiredFieldNoDefault;
                        }
                        """));

        assertThat(compilation).failed();
        assertThat(compilation)
                .hadErrorContaining(
                        "has a @Source but is not dynamically initializable because the following required properties lack default values");
    }

    @Test
    void testNotDynamicallyInitializableWarning() {
        Compilation compilation = javac().withProcessors(new ConfigProcessor())
                .compile(JavaFileObjects.forSourceString(
                        "me.bristermitten.mittenlib.tests.NotDynInitWarnConfigDTO", """
                        package me.bristermitten.mittenlib.tests;
                        import me.bristermitten.mittenlib.config.*;

                        @Source("config.yml")
                        @Config(requireDynamicInitialization = false)
                        public class NotDynInitWarnConfigDTO {
                            public String requiredFieldNoDefault;
                        }
                        """));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .hadWarningContaining(
                        "has a @Source but is not dynamically initializable because the following required properties lack default values");
    }

    @Test
    void testUnionAlternativeExtendingOtherClassNotUnion() {
        Compilation compilation = javac().withProcessors(new ConfigProcessor())
                .compile(JavaFileObjects.forSourceString("me.bristermitten.mittenlib.tests.UnionConfigDTO", """
                        package me.bristermitten.mittenlib.tests;
                        import me.bristermitten.mittenlib.config.*;

                        @Config
                        @ConfigUnion
                        public class UnionConfigDTO {
                            public int sharedProperty;

                            public static class SomeOtherClass {}

                            @Config
                            public static class AlternativeOneDTO extends SomeOtherClass {
                                public String altProperty;
                            }
                        }
                        """));

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("UnionConfigDTO MUST extend the union type");
    }
}
