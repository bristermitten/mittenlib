package me.bristermitten.mittenlib.annotations.config;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.CompilationSubject;
import com.google.testing.compile.JavaFileObjects;
import com.squareup.javapoet.ClassName;
import me.bristermitten.mittenlib.annotations.compile.ConfigurationClassNameGenerator;
import org.junit.jupiter.api.Test;

import static com.google.testing.compile.Compiler.javac;
import static org.assertj.core.api.Assertions.assertThat;

class ConfigurationClassNameGeneratorTest {

    @Test
    void generateFullConfigClassName() {
        Compilation compilation = javac()
                .withProcessors(new ConfigProcessor())
                .compile(JavaFileObjects.forSourceString("me.bristermitten.mittenlib.tests.LangConfigDTO",
                        """
                                package me.bristermitten.mittenlib.tests;
                                
                                import me.bristermitten.mittenlib.config.Config;
                                import me.bristermitten.mittenlib.config.Source;
                                import me.bristermitten.mittenlib.config.extension.UseObjectMapperSerialization;import me.bristermitten.mittenlib.config.names.NamingPattern;
                                import me.bristermitten.mittenlib.lang.LangMessage;
                                
                                @NamingPattern(value = me.bristermitten.mittenlib.config.names.NamingPatterns.LOWER_KEBAB_CASE)
                                @Source(value = "lang.yml")
                                @Config
                                @UseObjectMapperSerialization
                                public class LangConfigDTO {
                                    public final ErrorsDTO errors = null;
                                    public final CommandsDTO commands = null;
                                
                                    @Config
                                    public static class CommandsDTO {
                                        public final SelectionDTO selection = null;
                                
                                        @Config
                                        public static class SelectionDTO {
                                            public final LangMessage rename = null;
                                            public final LangMessage created = null;
                                            public final LangMessage deleted = null;
                                            public final LangMessage addedZone = null;
                                            public final LangMessage removedZone = null;
                                        }
                                    }
                                
                                    @Config
                                    public static class ErrorsDTO {
                                        public final LangMessage noSelection = null;
                                        public final SelectionDTO selection = null;
                                
                                        @Config
                                        public static class SelectionDTO {
                                            public final LangMessage nodeExists = null;
                                            public final LangMessage alreadyHaveSelection = null;
                                            public final LangMessage duplicateZone = null;
                                            public final LangMessage zoneNotPresent = null;
                                        }
                                    }
                                }
                                """));

        CompilationSubject.assertThat(compilation).succeededWithoutWarnings();
    }

    @Test
    void testTranslateConfigClassName() {
        ClassName dtoName = ClassName.bestGuess("TestConfigDTO");

        assertThat(ConfigurationClassNameGenerator.translateConfigClassName(dtoName))
                .isEqualTo(ClassName.bestGuess("TestConfig"));

        ClassName configName = ClassName.bestGuess("TestConfig");
        assertThat(ConfigurationClassNameGenerator.translateConfigClassName(configName))
                .isEqualTo(ClassName.bestGuess("TestConfigImpl"));
    }
}
