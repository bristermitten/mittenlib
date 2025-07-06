package me.bristermitten.mittenlib.annotations.config;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

/**
 * Tests the deserialization of primitive types in configuration classes.
 */
class PrimitiveTypesDeserializationTest {

    @Test
    void testPrimitiveTypesDeserialization() {
        Compilation compilation = javac()
                .withProcessors(new ConfigProcessor())
                .compile(JavaFileObjects.forSourceString("me.bristermitten.mittenlib.tests.PrimitiveTypesConfigDTO",
                        """
                                package me.bristermitten.mittenlib.tests;
                                
                                import me.bristermitten.mittenlib.config.Config;
                                import me.bristermitten.mittenlib.config.Source;
                                import me.bristermitten.mittenlib.config.names.NamingPattern;
                                import me.bristermitten.mittenlib.config.names.NamingPatterns;
                                
                                @NamingPattern(NamingPatterns.LOWER_KEBAB_CASE)
                                @Source("primitives.yml")
                                @Config
                                public class PrimitiveTypesConfigDTO {
                                    // Primitive types
                                    public int intValue = 42;
                                    public long longValue = 1234567890L;
                                    public double doubleValue = 3.14159;
                                    public float floatValue = 2.71828f;
                                    public boolean booleanValue = true;
                                    public char charValue = 'A';
                                    public byte byteValue = 127;
                                    public short shortValue = 32767;
                                
                                    // Boxed primitive types
                                    public Integer boxedIntValue = 42;
                                    public Long boxedLongValue = 1234567890L;
                                    public Double boxedDoubleValue = 3.14159;
                                    public Float boxedFloatValue = 2.71828f;
                                    public Boolean boxedBooleanValue = true;
                                    public Character boxedCharValue = 'A';
                                    public Byte boxedByteValue = 127;
                                    public Short boxedShortValue = 32767;
                                
                                    // String (not a primitive but commonly used)
                                    public String stringValue = "Hello, World!";
                                }
                                """));

        assertThat(compilation).succeededWithoutWarnings();
        
        // Verify that the generated class exists
        assertThat(compilation).generatedSourceFile("me.bristermitten.mittenlib.tests.PrimitiveTypesConfig")
                .isNotNull();
    }
}