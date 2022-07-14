package me.bristermitten.mittenlib.annotations.config;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class OverriddenNameGeneratorTest {

    @Test
    void generateFullConfigClassName() {
        Compilation compilation = javac()
                .withProcessors(new ConfigProcessor())
                .compile(JavaFileObjects.forSourceString("me.bristermitten.mittenlib.tests.OverriddenNameDTO",
                        """
                                package me.bristermitten.mittenlib.tests;
                                import java.util.Map;
                                import me.bristermitten.mittenlib.config.*;
                                @Config
                                public final class OverriddenNameDTO {
                                    public int clone;
                                }
                                """));

        assertThat(compilation).succeededWithoutWarnings();
    }
}
