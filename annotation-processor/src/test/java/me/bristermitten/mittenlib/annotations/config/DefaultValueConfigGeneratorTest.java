package me.bristermitten.mittenlib.annotations.config;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import me.bristermitten.mittenlib.annotations.compile.ConfigProcessor;
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
                                import                                 me.bristermitten.mittenlib.config.Config;
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
}
