package me.bristermitten.mittenlib.annotations.config;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;
import java.io.IOException;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static javax.tools.StandardLocation.CLASS_OUTPUT;

class DTOSuperclassTransformationTest {

    @Test
    void generateFullConfigClassName() throws IOException {
        Compilation compilation = javac()
                .withProcessors(new ConfigProcessor())
                .compile(JavaFileObjects.forSourceString("me.bristermitten.mittenlib.tests.SuperclassConfigDTO",
                        """
                                package me.bristermitten.mittenlib.tests;
                                                                
                                import java.util.Map;
                                                                
                                import me.bristermitten.mittenlib.config.*;
                                import me.bristermitten.mittenlib.config.names.*;
                                                                
                                @NamingPattern(value = me.bristermitten.mittenlib.config.names.NamingPatterns.LOWER_KEBAB_CASE)
                                @Source(value = "lang.yml")
                                @Config
                                public final class SuperclassConfigDTO {
                                    public Child1DTO child1;
                                    public Child2DTO child2;
                                                                
                                    @Config
                                    static class Child1DTO {
                                        int a;
                                    }
                                                                
                                    @Config
                                    static class Child2DTO extends Child1DTO {
                                        int b;
                                    }
                                }
                                """));

        assertThat(compilation).succeededWithoutWarnings();

        assertThat(compilation).generatedFile(CLASS_OUTPUT, "me/bristermitten/mittenlib/tests/SuperclassConfig.class")
                .isNotNull();

        for (JavaFileObject generatedSourceFile : compilation.generatedSourceFiles()) {
            System.out.println(generatedSourceFile.getCharContent(true));
        }

        // TODO figure out a proper way of testing contents -- AST comparison doesn't work as of java 17

    }
}

