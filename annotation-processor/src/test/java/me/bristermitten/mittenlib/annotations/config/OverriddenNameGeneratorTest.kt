package me.bristermitten.mittenlib.annotations.config

import com.google.testing.compile.CompilationSubject
import com.google.testing.compile.Compiler
import com.google.testing.compile.JavaFileObjects
import org.junit.jupiter.api.Test

internal class OverriddenNameGeneratorTest {
    @Test
    fun generateFullConfigClassName() {
        val compilation = Compiler.javac()
            .withProcessors(ConfigProcessor())
            .compile(
                JavaFileObjects.forSourceString(
                    "me.bristermitten.mittenlib.tests.OverriddenNameDTO",
                    """
                                package me.bristermitten.mittenlib.tests;
                                import java.util.Map;
                                import me.bristermitten.mittenlib.config.*;
                                @Config
                                public final class OverriddenNameDTO {
                                    public int clone;
                                }
                                
                                """.trimIndent()
                )
            )
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
    }
}
