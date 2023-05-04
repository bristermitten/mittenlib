package me.bristermitten.mittenlib.annotations.config

import com.google.testing.compile.Compiler
import com.google.testing.compile.JavaFileObjectSubject
import com.google.testing.compile.JavaFileObjects
import me.bristermitten.mittenlib.config.names.NamingPatterns
import org.junit.jupiter.api.Test
import javax.tools.JavaFileObject

internal class FieldClassNameGeneratorTest {
    private fun compileField(source: String, pattern: NamingPatterns? = null): JavaFileObject {
        val compilation = Compiler.javac()
            .withProcessors(ConfigProcessor())
            .compile(
                JavaFileObjects.forSourceString(
                    "me.bristermitten.mittenlib.tests.FieldClassNameGeneratorTestDTO", """
                        package me.bristermitten.mittenlib.tests;
                                                
                        import java.util.Map;
                                                
                        import me.bristermitten.mittenlib.config.*;
                        import me.bristermitten.mittenlib.config.names.*;
                        @Config
                        %s
                        public class FieldClassNameGeneratorTestDTO {
                        %s
                        }
                        
                        """.trimIndent().formatted(
                        if (pattern == null) "" else "@NamingPattern(NamingPatterns." + pattern.name + ")",
                        source
                    )
                )
            )
        return compilation.generatedSourceFile("me.bristermitten.mittenlib.tests.FieldClassNameGeneratorTest")
            .orElseThrow()
    }

    private fun assertConfigKeyUsed(source: JavaFileObject, key: String) {
        JavaFileObjectSubject.assertThat(source)
            .contentsAsUtf8String()
            .contains("\$data.getOrDefault(\"%s\"".formatted(key))
    }

    @Test
    fun assertThat_unannotatedFieldName_isIdentity() {
        val source = compileField("int hello;")
        assertConfigKeyUsed(source, "hello")
    }

    @Test
    fun assertThat_annotatedFieldName_hasHigherPriority_withConfigName() {
        val source = compileField(
            """
                @ConfigName("field-name")
                int hello;
                
                """.trimIndent()
        )
        assertConfigKeyUsed(source, "field-name")
    }

    @Test
    fun assertThat_annotatedConfigName_hasHigherPriority_thanNamingPattern() {
        val source = compileField(
            """
                @ConfigName("field-name")
                int hello;
                
                """.trimIndent(), NamingPatterns.LOWER_SNAKE_CASE
        )
        assertConfigKeyUsed(source, "field-name")
    }

    @Test
    fun assertThat_field_with_NamingPattern_hasHigherPriority_than_class_with_NamingPattern() {
        val source = compileField(
            """
                @NamingPattern(NamingPatterns.UPPER_CAMEL_CASE)
                int fieldName;
                
                """.trimIndent(), NamingPatterns.LOWER_SNAKE_CASE
        )
        assertConfigKeyUsed(source, "FieldName")
    }

    @Test
    fun assertThat_unannotatedFieldName_usesClass_withNamingPattern() {
        val source = compileField(
            """
                int fieldName;
                
                """.trimIndent(), NamingPatterns.LOWER_KEBAB_CASE
        )
        assertConfigKeyUsed(source, "field-name")
    }
}
