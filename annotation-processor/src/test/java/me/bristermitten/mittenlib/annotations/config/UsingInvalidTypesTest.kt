package me.bristermitten.mittenlib.annotations.config

import com.google.testing.compile.Compiler
import com.google.testing.compile.JavaFileObjects
import me.bristermitten.mittenlib.annotations.exception.DTOReferenceException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class UsingInvalidTypesTest {
    @Test
    fun generateConfigReferencingGeneratedType() {
        val compilation = Compiler.javac()
            .withProcessors(ConfigProcessor())
        val source1 = JavaFileObjects.forSourceString(
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
        val source2 = JavaFileObjects.forSourceString(
            "me.bristermitten.mittenlib.tests.OtherDTO",
            """
                        package me.bristermitten.mittenlib.tests;
                        import java.util.Map;
                        import me.bristermitten.mittenlib.config.*;
                        @Config
                        public final class OtherDTO {
                            public me.bristermitten.mittenlib.tests.OverriddenName fail;
                        }
                        
                        """.trimIndent()
        )
        val exception =
            Assertions.assertThrows(RuntimeException::class.java) { compilation.compile(source1, source2) }
        Assertions.assertTrue(exception.cause is DTOReferenceException)
    }

    @Test
    fun generateConfigReferencingNonExistentType() {
        val compilation = Compiler.javac()
            .withProcessors(ConfigProcessor())
        val source1 = JavaFileObjects.forSourceString(
            "me.bristermitten.mittenlib.tests.OverriddenNameDTO",
            """
                        package me.bristermitten.mittenlib.tests;
                        import java.util.Map;
                        import me.bristermitten.mittenlib.config.*;
                        @Config
                        public final class OverriddenNameDTO {
                            public Blushiwudhqiuhqi what;
                        }
                        
                        """.trimIndent()
        )
        val exception = Assertions.assertThrows(RuntimeException::class.java) { compilation.compile(source1) }
        Assertions.assertTrue(exception.cause is DTOReferenceException)
    }
}