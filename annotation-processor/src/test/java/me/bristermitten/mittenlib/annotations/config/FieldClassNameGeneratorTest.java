package me.bristermitten.mittenlib.annotations.config;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import me.bristermitten.mittenlib.config.names.NamingPatterns;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import javax.tools.JavaFileObject;

import static com.google.testing.compile.Compiler.javac;
import static com.google.testing.compile.JavaFileObjectSubject.assertThat;

class FieldClassNameGeneratorTest {

    private JavaFileObject compileField(String source) {
        return compileField(source, null);
    }

    private JavaFileObject compileField(String source, @Nullable NamingPatterns pattern) {
        Compilation compilation = javac()
                .withProcessors(new ConfigProcessor())
                .compile(JavaFileObjects.forSourceString("me.bristermitten.mittenlib.tests.FieldClassNameGeneratorTestDTO", """
                        package me.bristermitten.mittenlib.tests;
                        import java.util.Map;
                                                
                        import me.bristermitten.mittenlib.config.*;
                        import me.bristermitten.mittenlib.config.names.*;
                        @Config
                        %s
                        public class FieldClassNameGeneratorTestDTO {
                        %s
                        }
                        """.formatted(
                        pattern == null ? "" : "@NamingPattern(NamingPatterns." + pattern.name() + ")",
                        source)));

        return compilation.generatedSourceFile("me.bristermitten.mittenlib.tests.FieldClassNameGeneratorTest")
                .orElseThrow();
    }

    private void assertConfigKeyUsed(JavaFileObject source, String key) {
        assertThat(source)
                .contentsAsUtf8String()
                .contains("$data.get(\"%s\"".formatted(key));
    }

    @Test
    void assertThat_unannotatedFieldName_isIdentity() {
        var source = compileField("int hello;");
        assertConfigKeyUsed(source, "hello");
    }


    @Test
    void assertThat_annotatedFieldName_hasHigherPriority_withConfigName() {
        var source = compileField("""
                @ConfigName("field-name")
                int hello;
                """);
        assertConfigKeyUsed(source, "field-name");
    }

    @Test
    void assertThat_annotatedConfigName_hasHigherPriority_thanNamingPattern() {
        var source = compileField("""
                @ConfigName("field-name")
                int hello;
                """, NamingPatterns.LOWER_SNAKE_CASE);
        assertConfigKeyUsed(source, "field-name");
    }

    @Test
    void assertThat_field_with_NamingPattern_hasHigherPriority_than_class_with_NamingPattern() {
        var source = compileField("""
                @NamingPattern(NamingPatterns.UPPER_CAMEL_CASE)
                int fieldName;
                """, NamingPatterns.LOWER_SNAKE_CASE);
        assertConfigKeyUsed(source, "FieldName");
    }

    @Test
    void assertThat_unannotatedFieldName_usesClass_withNamingPattern() {
        var source = compileField("""
                int fieldName;
                """, NamingPatterns.LOWER_KEBAB_CASE);
        assertConfigKeyUsed(source, "field-name");
    }

}
