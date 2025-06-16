package me.bristermitten.mittenlib.annotations.config;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

/**
 * Tests the deserialization of nullable fields in configuration classes.
 */
class NullableFieldsDeserializationTest {

    @Test
    void testNullableFieldsDeserialization() {
        Compilation compilation = javac()
                .withProcessors(new ConfigProcessor())
                .compile(JavaFileObjects.forSourceString("me.bristermitten.mittenlib.tests.NullableFieldsConfigDTO",
                        """
                                package me.bristermitten.mittenlib.tests;
                                
                                import me.bristermitten.mittenlib.config.Config;
                                import me.bristermitten.mittenlib.config.Source;
                                import me.bristermitten.mittenlib.config.names.NamingPattern;
                                import me.bristermitten.mittenlib.config.names.NamingPatterns;
                                import org.jetbrains.annotations.Nullable;
                                
                                import java.util.List;
                                import java.util.Map;
                                
                                @NamingPattern(NamingPatterns.LOWER_KEBAB_CASE)
                                @Source("nullable.yml")
                                @Config
                                public class NullableFieldsConfigDTO {
                                    // Required fields (not nullable)
                                    public int requiredInt = 42;
                                    public String requiredString = "Required";
                                    
                                    // Nullable primitive wrappers
                                    @Nullable public Integer nullableInt = null;
                                    @Nullable public Double nullableDouble = null;
                                    @Nullable public Boolean nullableBoolean = null;
                                    
                                    // Nullable objects
                                    @Nullable public String nullableString = null;
                                    @Nullable public List<String> nullableList = null;
                                    @Nullable public Map<String, Integer> nullableMap = null;
                                    
                                    // Nullable nested config
                                    @Nullable public NestedConfigDTO nullableNestedConfig = null;
                                    
                                    @Config
                                    public static class NestedConfigDTO {
                                        public int value = 100;
                                        @Nullable public String description = null;
                                    }
                                }
                                """));

        assertThat(compilation).succeededWithoutWarnings();
        
        // Verify that the generated class exists
        assertThat(compilation).generatedSourceFile("me.bristermitten.mittenlib.tests.NullableFieldsConfig")
                .isNotNull();
                
        // Verify that the generated nested class exists
        assertThat(compilation).generatedSourceFile("me.bristermitten.mittenlib.tests.NullableFieldsConfig")
                .contentsAsUtf8String()
                .contains("static class NestedConfig");
    }
}