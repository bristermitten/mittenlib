package me.bristermitten.mittenlib.annotations.config;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
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
                                import java.util.Map;
                                import me.bristermitten.mittenlib.config.*;
                                @Config
                                public final class DefaultValueConfigDTO {
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
