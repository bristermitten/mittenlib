package me.bristermitten.mittenlib.annotations.config;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NestedConfigBindingTest {

    @Test
    void testNestedConfigBindingsAreGenerated() throws IOException {
        Compilation compilation = javac().withProcessors(new ConfigProcessor())
                .compile(JavaFileObjects.forSourceString("me.bristermitten.mittenlib.tests.RootConfigDTO", """
                                package me.bristermitten.mittenlib.tests;

                                import me.bristermitten.mittenlib.config.Config;
                                import me.bristermitten.mittenlib.config.Source;

                                @Source("config.yml")
                                @Config(requireDynamicInitialization = false)
                                public class RootConfigDTO {
                                    public ChildConfigDTO child;

                                    @Config
                                    public static class ChildConfigDTO {
                                        public int i;
                                        public GrandChildConfigDTO grandChild;

                                        @Config
                                        public static class GrandChildConfigDTO {
                                            public String s;
                                        }
                                    }
                                }
                                """));

        assertThat(compilation).succeeded();

        var module = compilation.generatedSourceFile("me.bristermitten.mittenlib.tests.ConfigLoaderModule");
        assertTrue(module.isPresent(), "ConfigLoaderModule was not generated");

        String content = module.get().getCharContent(true).toString();
        assertTrue(
                content.contains("public RootConfig.ChildConfig provideChildConfig(RootConfig parent)"),
                "Missing provideChildConfig method");
        assertTrue(content.contains("return parent.child();"), "Missing return parent.child() statement");
        assertTrue(
                content.contains("public RootConfig.ChildConfig.GrandChildConfig provideGrandChildConfig("),
                "Missing provideGrandChildConfig method");
        assertTrue(content.contains("return parent.grandChild();"), "Missing return parent.grandChild() statement");
    }

    @Test
    void testNestedInterfaceConfigBindingsAreGenerated() throws IOException {
        Compilation compilation = javac().withProcessors(new ConfigProcessor())
                .compile(JavaFileObjects.forSourceString("me.bristermitten.mittenlib.tests.RootInterfaceConfig", """
                                package me.bristermitten.mittenlib.tests;

                                import me.bristermitten.mittenlib.config.Config;
                                import me.bristermitten.mittenlib.config.Source;

                                @Source("config.yml")
                                @Config(requireDynamicInitialization = false)
                                public interface RootInterfaceConfig {
                                    ChildInterface child();

                                    @Config
                                    interface ChildInterface {
                                        int i();
                                    }
                                }
                                """));

        assertThat(compilation).succeeded();

        var module = compilation.generatedSourceFile("me.bristermitten.mittenlib.tests.ConfigLoaderModule");
        assertTrue(module.isPresent(), "ConfigLoaderModule was not generated");

        String content = module.get().getCharContent(true).toString();
        assertTrue(
                content.contains(
                        "public RootInterfaceConfig.ChildInterface provideChildInterface(RootInterfaceConfig parent)"),
                "Missing provideChildInterface method");
        assertTrue(content.contains("return parent.child();"), "Missing return parent.child() statement");
    }

    @Test
    void testAmbiguousNestedConfigBindingsAreNotGenerated() throws IOException {
        Compilation compilation = javac().withProcessors(new ConfigProcessor())
                .compile(JavaFileObjects.forSourceString("me.bristermitten.mittenlib.tests.AmbiguousConfigDTO", """
                                package me.bristermitten.mittenlib.tests;

                                import me.bristermitten.mittenlib.config.Config;
                                import me.bristermitten.mittenlib.config.Source;

                                @Source("config.yml")
                                @Config(requireDynamicInitialization = false)
                                public class AmbiguousConfigDTO {
                                    public ChildConfigDTO first;
                                    public ChildConfigDTO second;

                                    @Config
                                    public static class ChildConfigDTO {
                                        public int i;
                                    }
                                }
                                """));

        assertThat(compilation).succeeded();

        var module = compilation.generatedSourceFile("me.bristermitten.mittenlib.tests.ConfigLoaderModule");
        assertTrue(module.isPresent(), "ConfigLoaderModule was not generated");

        String content = module.get().getCharContent(true).toString();
        assertFalse(content.contains("provideChildConfig"), "Should not have provideChildConfig due to ambiguity");
    }

    @Test
    void testAmbiguousNestedConfigBindingsWithOverrideAreGenerated() throws IOException {
        Compilation compilation = javac().withProcessors(new ConfigProcessor())
                .compile(JavaFileObjects.forSourceString(
                        "me.bristermitten.mittenlib.tests.OverrideAmbiguousConfigDTO", """
                                package me.bristermitten.mittenlib.tests;

                                import me.bristermitten.mittenlib.config.Config;
                                import me.bristermitten.mittenlib.config.Source;
                                import me.bristermitten.mittenlib.config.BindProperty;

                                @Source("config.yml")
                                @Config(requireDynamicInitialization = false)
                                public class OverrideAmbiguousConfigDTO {
                                    @BindProperty
                                    public ChildConfigDTO first;
                                    public ChildConfigDTO second;

                                    @Config
                                    public static class ChildConfigDTO {
                                        public int i;
                                    }
                                }
                                """));

        assertThat(compilation).succeeded();

        var module = compilation.generatedSourceFile("me.bristermitten.mittenlib.tests.ConfigLoaderModule");
        assertTrue(module.isPresent(), "ConfigLoaderModule was not generated");

        String content = module.get().getCharContent(true).toString();
        assertTrue(
                content.contains(
                        "public OverrideAmbiguousConfig.ChildConfig provideChildConfig(OverrideAmbiguousConfig parent)"),
                "Missing provideChildConfig method");
        assertTrue(content.contains("return parent.first();"), "Should bind to 'first' property");
    }

    @Test
    void testMultipleBindPropertyAnnotationsFails() {
        Compilation compilation = javac().withProcessors(new ConfigProcessor())
                .compile(JavaFileObjects.forSourceString(
                        "me.bristermitten.mittenlib.tests.DoubleOverrideConfigDTO", """
                                package me.bristermitten.mittenlib.tests;

                                import me.bristermitten.mittenlib.config.Config;
                                import me.bristermitten.mittenlib.config.Source;
                                import me.bristermitten.mittenlib.config.BindProperty;

                                @Source("config.yml")
                                @Config(requireDynamicInitialization = false)
                                public class DoubleOverrideConfigDTO {
                                    @BindProperty
                                    public ChildConfigDTO first;
                                    @BindProperty
                                    public ChildConfigDTO second;

                                    @Config
                                    public static class ChildConfigDTO {
                                        public int i;
                                    }
                                }
                                """));

        assertThat(compilation).failed();
        assertThat(compilation)
                .hadErrorContaining("Multiple properties of type ChildConfig are marked with @BindProperty");
    }
}
