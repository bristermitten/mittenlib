package me.bristermitten.mittenlib.annotations.config;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class ConfigClassNameGeneratorTest {

    @Test
    void generateFullConfigClassName() {
        Compilation compilation = javac()
                .withProcessors(new ConfigProcessor())
                .compile(JavaFileObjects.forSourceString("me.bristermitten.mittenlib.tests.LangConfigDTO",
                        """
                                package me.bristermitten.mittenlib.tests;
                                @me.bristermitten.mittenlib.config.names.NamingPattern(value = me.bristermitten.mittenlib.config.names.NamingPatterns.LOWER_KEBAB_CASE)
                                @me.bristermitten.mittenlib.config.Source(value = "lang.yml")
                                @me.bristermitten.mittenlib.config.Config
                                public final class LangConfigDTO {
                                    public final ErrorsDTO errors = null;
                                    public final CommandsDTO commands = null;
                                    @me.bristermitten.mittenlib.config.Config
                                    public static final class CommandsDTO {
                                        public final SelectionDTO selection = null;
                                        @me.bristermitten.mittenlib.config.Config
                                        public static final class SelectionDTO {
                                            public final me.bristermitten.mittenlib.lang.LangMessage rename = null;
                                            public final me.bristermitten.mittenlib.lang.LangMessage created = null;
                                            public final me.bristermitten.mittenlib.lang.LangMessage deleted = null;
                                            public final me.bristermitten.mittenlib.lang.LangMessage addedZone = null;
                                            public final me.bristermitten.mittenlib.lang.LangMessage removedZone = null;
                                        }
                                    }
                                    @me.bristermitten.mittenlib.config.Config
                                    public static final class ErrorsDTO {
                                        public final me.bristermitten.mittenlib.lang.LangMessage noSelection = null;
                                        public final SelectionDTO selection = null;
                                        @me.bristermitten.mittenlib.config.Config
                                        public static final class SelectionDTO {
                                            public final me.bristermitten.mittenlib.lang.LangMessage nodeExists = null;
                                            public final me.bristermitten.mittenlib.lang.LangMessage alreadyHaveSelection = null;
                                            public final me.bristermitten.mittenlib.lang.LangMessage duplicateZone = null;
                                            public final me.bristermitten.mittenlib.lang.LangMessage zoneNotPresent = null;
                                        }
                                    }
                                }
                                """));

        assertThat(compilation).succeededWithoutWarnings();
    }
}
