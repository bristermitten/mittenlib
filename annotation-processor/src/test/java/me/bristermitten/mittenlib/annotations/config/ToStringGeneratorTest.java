package me.bristermitten.mittenlib.annotations.config;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class ToStringGeneratorTest {

    @Test
    void generatesToStringMethod() {
        Compilation compilation = javac()
                .withProcessors(new ConfigProcessor())
                .compile(JavaFileObjects.forSourceString("me.bristermitten.mittenlib.tests.ToStringConfigDTO",
                        """
                                package me.bristermitten.mittenlib.tests;
                                import java.util.Map;
                                import me.bristermitten.mittenlib.config.*;import me.bristermitten.mittenlib.config.generate.GenerateToString;
                                @Config
                                @GenerateToString
                                public class ToStringConfigDTO {
                                    int x = 3;
                                    int y;
                                    String z;
                                }
                                """));

        assertThat(compilation).succeededWithoutWarnings();
        assertThat(compilation).generatedSourceFile("me.bristermitten.mittenlib.tests.ToStringConfig")
                .isNotNull();
        assertThat(compilation).generatedSourceFile("me.bristermitten.mittenlib.tests.ToStringConfig")
                .contentsAsUtf8String()
                .contains("""
                          @Override
                          public String toString() {
                            return "ToStringConfig{" + "x=" + x + "," + "y=" + y + "," + "z=" + z + "}";
                          }
                        """);
    }

    @Test
    void generatesToStringMethodWithSubclass() {
        Compilation compilation = javac()
                .withProcessors(new ConfigProcessor())
                .compile(JavaFileObjects.forSourceString("me.bristermitten.mittenlib.tests.ToStringConfigDTO",
                        """
                                package me.bristermitten.mittenlib.tests;
                                
                                import me.bristermitten.mittenlib.config.Config;
                                import me.bristermitten.mittenlib.config.generate.GenerateToString;
                                
                                @Config
                                @GenerateToString
                                public class ToStringConfigDTO {
                                    int x = 3;
                                
                                    @Config
                                    public static class SubclassDTO {
                                        int y = 4;
                                    }
                                }
                                """));

        assertThat(compilation).succeededWithoutWarnings();
        assertThat(compilation).generatedSourceFile("me.bristermitten.mittenlib.tests.ToStringConfig")
                .isNotNull();
        assertThat(compilation).generatedSourceFile("me.bristermitten.mittenlib.tests.ToStringConfig")
                .contentsAsUtf8String()
                .containsMatch("""
                        \\s+@Override
                        \\s+public String toString\\(\\) \\{
                        \\s+return "Subclass\\{" \\+ "y=" \\+ y \\+ "}";
                        \\s+}
                        """);
    }
}
