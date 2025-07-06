package me.bristermitten.mittenlib.annotations.config;

import com.google.testing.compile.JavaFileObjects;
import me.bristermitten.mittenlib.annotations.exception.DTOReferenceException;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

import static com.google.testing.compile.Compiler.javac;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsingInvalidTypesTest {

    @Test
    void generateConfigReferencingGeneratedType() {
        var compilation = javac()
                .withProcessors(new ConfigProcessor());

        JavaFileObject source1 = JavaFileObjects.forSourceString("me.bristermitten.mittenlib.tests.OverriddenNameDTO",
                """
                        package me.bristermitten.mittenlib.tests;
                        import java.util.Map;
                        import me.bristermitten.mittenlib.config.*;
                        @Config
                        public class OverriddenNameDTO {
                            public int clone;
                        }
                        """);
        JavaFileObject source2 = JavaFileObjects.forSourceString("me.bristermitten.mittenlib.tests.OtherDTO",
                """
                        package me.bristermitten.mittenlib.tests;
                        import java.util.Map;
                        import me.bristermitten.mittenlib.config.*;
                        @Config
                        public class OtherDTO {
                            public me.bristermitten.mittenlib.tests.OverriddenName fail;
                        }
                        """);


        assertThatThrownBy(
                () -> compilation.compile(source1, source2)
        ).hasCauseInstanceOf(DTOReferenceException.class);
    }

    @Test
    void generateConfigReferencingNonExistentType() {
        var compilation = javac()
                .withProcessors(new ConfigProcessor());

        JavaFileObject source1 = JavaFileObjects.forSourceString("me.bristermitten.mittenlib.tests.OverriddenNameDTO",
                """
                        package me.bristermitten.mittenlib.tests;
                        import java.util.Map;
                        import me.bristermitten.mittenlib.config.*;
                        @Config
                        public class OverriddenNameDTO {
                            public DefinitelyAnInvalidNameIHope what;
                        }
                        """);

        assertThatThrownBy(
                () -> compilation.compile(source1)
        ).hasCauseInstanceOf(DTOReferenceException.class)
                .hasMessageContaining("DefinitelyAnInvalidNameIHope");
    }
}
