package me.bristermitten.mittenlib.annotations.config;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import io.toolisticon.cute.Cute;
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
                                public class OverriddenNameDTO {
                                    public int clone;
                                }
                                """));

        assertThat(compilation).succeededWithoutWarnings();
    }

    @Test
    void testOverriddenConfigName() {
        Cute.blackBoxTest()
                .given()
                .processor(ConfigProcessor.class)
                .andSourceFile("OverriddenImplConfig", """
                        import me.bristermitten.mittenlib.config.Config;
                        @Config(className = "ThisIsTheImpl")
                        public interface OverriddenImplConfig {
                            int id();
                        }
                        """)
                .whenCompiled()
                .thenExpectThat()
                .compilationSucceeds()
                .andThat()
                .generatedClass("ThisIsTheImpl")
                .exists()
                .executeTest();
    }

    @Test
    void testNestedOverriddenConfigName() {
        Cute.blackBoxTest()
                .given()
                .processor(ConfigProcessor.class)
                .andSourceFile("OverriddenImplConfig", """
                        import me.bristermitten.mittenlib.config.Config;
                        
                        @Config(className = "ThisIsTheImpl")
                        public interface OverriddenImplConfig {
                            int id();
                        
                            @Config
                            interface NormalSubConfig {
                                int id2();
                            }
                        }
                        """)
                .whenCompiled()
                .thenExpectThat()
                .compilationSucceeds()
                .andThat()
                .generatedClass("ThisIsTheImpl")
                .exists()
                .executeTest();
    }
}
