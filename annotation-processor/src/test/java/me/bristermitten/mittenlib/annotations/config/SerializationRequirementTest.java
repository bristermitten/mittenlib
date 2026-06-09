package me.bristermitten.mittenlib.annotations.config;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class SerializationRequirementTest {

    @Test
    void testSerializationRequirementFails() {
        Compilation compilation = javac().withProcessors(new ConfigProcessor())
                .compile(JavaFileObjects.forSourceString(
                        "me.bristermitten.mittenlib.tests.UnserializableConfigDTO", """
                                package me.bristermitten.mittenlib.tests;

                                import me.bristermitten.mittenlib.config.Config;

                                @Config(requireSerialization = true)
                                public class UnserializableConfigDTO {
                                    public Object unsupported;
                                }
                                """));

        assertThat(compilation).failed();
        assertThat(compilation)
                .hadErrorContaining(
                        "Serialization is required for this config, but it contains properties that cannot be serialized");
    }

    @Test
    void testSerializationRequirementDefaultsToTrueAndFails() {
        Compilation compilation = javac().withProcessors(new ConfigProcessor())
                .compile(JavaFileObjects.forSourceString(
                        "me.bristermitten.mittenlib.tests.UnserializableDefaultConfigDTO", """
                                package me.bristermitten.mittenlib.tests;

                                import me.bristermitten.mittenlib.config.Config;

                                @Config
                                public class UnserializableDefaultConfigDTO {
                                    public Object unsupported;
                                }
                                """));

        assertThat(compilation).failed();
        assertThat(compilation)
                .hadErrorContaining(
                        "Serialization is required for this config, but it contains properties that cannot be serialized");
    }

    @Test
    void testSerializationWarningWhenNotRequired() {
        Compilation compilation = javac().withProcessors(new ConfigProcessor())
                .compile(JavaFileObjects.forSourceString(
                        "me.bristermitten.mittenlib.tests.UnserializableWarningConfigDTO", """
                                package me.bristermitten.mittenlib.tests;

                                import me.bristermitten.mittenlib.config.Config;

                                @Config(requireSerialization = false)
                                public class UnserializableWarningConfigDTO {
                                    public Object unsupported;
                                }
                                """));

        assertThat(compilation).succeeded();
        assertThat(compilation).hadWarningContaining("This config contains properties that cannot be serialized");
    }
}
