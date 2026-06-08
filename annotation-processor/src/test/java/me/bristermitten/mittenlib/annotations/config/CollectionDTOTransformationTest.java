package me.bristermitten.mittenlib.annotations.config;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class CollectionDTOTransformationTest {

    @Test
    void generateFullConfigClassName() {
        Compilation compilation = javac()
                .withProcessors(new ConfigProcessor())
                .compile(JavaFileObjects.forSourceString("me.bristermitten.mittenlib.tests.CollectionConfig",
                        """
                                package me.bristermitten.mittenlib.tests;
                                import me.bristermitten.mittenlib.config.Config;
                                import me.bristermitten.mittenlib.config.Source;
                                import me.bristermitten.mittenlib.config.names.NamingPattern;
                                import me.bristermitten.mittenlib.config.names.NamingPatterns;
                                
                                import java.util.Map;
                                @NamingPattern(value = NamingPatterns.LOWER_KEBAB_CASE)
                                @Source(value = "lang.yml")
                                @Config
                                public class CollectionConfig {
                                    public final Map<String, SubConfig> map = null;
                                    @Config
                                    public static class SubConfig {
                                        int i;
                                        String s;
                                    }
                                }
                                """));

        assertThat(compilation).succeeded();
    }
}

