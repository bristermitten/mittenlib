package me.bristermitten.mittenlib.annotations.compile;

import static com.google.testing.compile.Compiler.javac;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.CompilationSubject;
import com.google.testing.compile.JavaFileObjects;
import com.squareup.javapoet.ClassName;
import me.bristermitten.mittenlib.annotations.ast.ASTSettings;
import me.bristermitten.mittenlib.annotations.ast.AbstractConfigStructure;
import me.bristermitten.mittenlib.annotations.ast.ConfigTypeSource;
import me.bristermitten.mittenlib.annotations.config.ConfigProcessor;
import me.bristermitten.mittenlib.config.Config;
import org.junit.jupiter.api.Test;

class ConfigurationClassNameGeneratorTest {

    @Test
    void generateFullConfigClassName() {
        Compilation compilation = javac().withProcessors(new ConfigProcessor())
                .compile(JavaFileObjects.forSourceString("me.bristermitten.mittenlib.tests.LangConfigDTO", """
                                package me.bristermitten.mittenlib.tests;

                                import me.bristermitten.mittenlib.config.Config;
                                import me.bristermitten.mittenlib.config.Source;
                                import me.bristermitten.mittenlib.config.extension.UseObjectMapperSerialization;
                                import me.bristermitten.mittenlib.config.names.NamingPattern;
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

        CompilationSubject.assertThat(compilation).succeeded();
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

    @Test
    void getDeserializerClassName() {
        var ast = mock(AbstractConfigStructure.Atomic.class);
        var settings = mock(ASTSettings.ConfigASTSettings.class);
        when(ast.settings()).thenReturn(settings);

        var config = mock(Config.class);
        when(config.className()).thenReturn("");
        when(settings.config()).thenReturn(config);

        var source = mock(ConfigTypeSource.ClassConfigTypeSource.class);
        when(ast.source()).thenReturn(source);

        when(ast.name()).thenReturn(ClassName.bestGuess("TestConfig"));

        ConfigurationClassNameGenerator generator = new ConfigurationClassNameGenerator(new ConfigNameCache());

        assertThat(generator.getDeserializerClassName(ast)).isEqualTo(ClassName.bestGuess("TestConfigDeserializer"));
    }
}
