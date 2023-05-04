package me.bristermitten.mittenlib.annotations.config

import com.google.testing.compile.CompilationSubject
import com.google.testing.compile.Compiler
import com.google.testing.compile.JavaFileObjects
import org.junit.jupiter.api.Test

internal class EmptyConfigGeneratorTest {
    @Test
    fun generateFullConfigClassName() {
        val compilation = Compiler.javac()
            .withProcessors(ConfigProcessor())
            .compile(
                JavaFileObjects.forSourceString(
                    "me.bristermitten.mittenlib.tests.EmptyConfigDTO",
                    """
                                package me.bristermitten.mittenlib.tests;
                                import java.util.Map;
                                import me.bristermitten.mittenlib.config.*;
                                @Config
                                public final class EmptyConfigDTO {
                                }
                                
                                """.trimIndent()
                )
            )
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
    }
}
