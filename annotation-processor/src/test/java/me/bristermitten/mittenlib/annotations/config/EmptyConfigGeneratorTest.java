package me.bristermitten.mittenlib.annotations.config;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import me.bristermitten.mittenlib.annotations.compile.ConfigProcessor;
import org.junit.jupiter.api.Test;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class EmptyConfigGeneratorTest {

    @Test
    void generateFullConfigClassName() {
        Compilation compilation = javac()
                .withProcessors(new ConfigProcessor())
                .compile(JavaFileObjects.forSourceString("me.bristermitten.mittenlib.tests.EmptyConfigDTO",
                        """
                                package me.bristermitten.mittenlib.tests;
                                import java.util.Map;
                                import me.bristermitten.mittenlib.config.*;
                                @Config
                                public class EmptyConfigDTO {
                                }
                                """));

        assertThat(compilation).succeededWithoutWarnings();
    }
}
