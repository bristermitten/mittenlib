package me.bristermitten.mittenlib.annotations.config

import com.google.testing.compile.CompilationSubject
import com.google.testing.compile.Compiler
import com.google.testing.compile.JavaFileObjects
import org.junit.jupiter.api.Test

internal class ToStringGeneratorTest {
    @Test
    fun generatesToStringMethod() {
        val compilation = Compiler.javac()
            .withProcessors(ConfigProcessor())
            .compile(
                JavaFileObjects.forSourceString(
                    "me.bristermitten.mittenlib.tests.ToStringConfigDTO",
                    """
                                package me.bristermitten.mittenlib.tests;
                                import java.util.Map;
                                import me.bristermitten.mittenlib.config.*;import me.bristermitten.mittenlib.config.generate.GenerateToString;
                                @Config
                                @GenerateToString
                                public final class ToStringConfigDTO {
                                    int x = 3;
                                    int y;
                                    String z;
                                }
                                
                                """.trimIndent()
                )
            )
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        CompilationSubject.assertThat(compilation)
            .generatedSourceFile("me.bristermitten.mittenlib.tests.ToStringConfig")
            .isNotNull()
        CompilationSubject.assertThat(compilation)
            .generatedSourceFile("me.bristermitten.mittenlib.tests.ToStringConfig")
            .contentsAsUtf8String()
            .contains(
                """
                public String toString()
                """.trimIndent()
            )
    }

    @Test
    fun generatesToStringMethodWithSubclass() {
        val compilation = Compiler.javac()
            .withProcessors(ConfigProcessor())
            .compile(
                JavaFileObjects.forSourceString(
                    "me.bristermitten.mittenlib.tests.ToStringConfigDTO",
                    """
                                package me.bristermitten.mittenlib.tests;
                                import java.util.Map;
                                import me.bristermitten.mittenlib.config.*;import me.bristermitten.mittenlib.config.generate.GenerateToString;
                                @Config
                                @GenerateToString
                                public final class ToStringConfigDTO {
                                    int x = 3;
                                    
                                    @Config
                                    public static class SubclassDTO {
                                      int y = 4;
                                    }
                                }
                                
                                """.trimIndent()
                )
            )
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        CompilationSubject.assertThat(compilation)
            .generatedSourceFile("me.bristermitten.mittenlib.tests.ToStringConfig")
            .isNotNull()
        CompilationSubject.assertThat(compilation)
            .generatedSourceFile("me.bristermitten.mittenlib.tests.ToStringConfig")
            .contentsAsUtf8String()
            .containsMatch(
                """
                        \s+@Override
                        \s+public String toString\(\) \{
                        \s+return "Subclass\{" \+ "y=" \+ y \+ "}";
                        \s+}
                        
                        """.trimIndent()
            )
    }
}
