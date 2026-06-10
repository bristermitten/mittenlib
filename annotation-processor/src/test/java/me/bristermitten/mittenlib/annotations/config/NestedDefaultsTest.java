package me.bristermitten.mittenlib.annotations.config;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

class NestedDefaultsTest {

    @Test
    void testNestedConfigWithDefaultsIsDynamicallyInitializable() {
        Compilation compilation = javac().withProcessors(new ConfigProcessor())
                .compile(JavaFileObjects.forSourceString(
                        "me.bristermitten.mittenlib.tests.NestedDefaultsConfigDTO", """
                                package me.bristermitten.mittenlib.tests;

                                import me.bristermitten.mittenlib.config.Config;
                                import me.bristermitten.mittenlib.config.Source;

                                @Source("config.yml")
                                @Config
                                public class NestedDefaultsConfigDTO {
                                    public NestedConfigDTO nestedConfig;

                                    @Config
                                    public static class NestedConfigDTO {
                                        public int value = 5;
                                    }
                                }
                                """));

        assertThat(compilation).succeeded();
    }

    @Test
    void testDeeplyNestedConfigWithDefaultsIsDynamicallyInitializable() {
        Compilation compilation = javac().withProcessors(new ConfigProcessor())
                .compile(JavaFileObjects.forSourceString(
                        "me.bristermitten.mittenlib.tests.DeeplyNestedDefaultsConfigDTO", """
                                package me.bristermitten.mittenlib.tests;

                                import me.bristermitten.mittenlib.config.Config;
                                import me.bristermitten.mittenlib.config.Source;

                                @Source("config.yml")
                                @Config
                                public class DeeplyNestedDefaultsConfigDTO {
                                    public Level1 level1;

                                    @Config
                                    public static class Level1 {
                                        public Level2 level2;

                                        @Config
                                        public static class Level2 {
                                            public int value = 5;
                                        }
                                    }
                                }
                                """));

        assertThat(compilation).succeeded();
    }
}
