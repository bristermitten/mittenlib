package me.bristermitten.mittenlib.annotations.config;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class DTOSuperclassTransformationTest {

    @Test
    void testSimpleSuperClassConfig() {
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

        assertThat(compilation).generatedSourceFile("me.bristermitten.mittenlib.tests.SuperclassConfig")
                .isNotNull();
        // TODO figure out a proper way of testing contents -- AST comparison doesn't work as of java 17

    }

    @Test
    void testMoreAdvancedClassConfig() {
        Compilation compilation = javac()
                .withProcessors(new ConfigProcessor())
                .compile(JavaFileObjects.forSourceString("me.bristermitten.mittenlib.tests.ShopConfigDTO",
                                """
                                        package me.bristermitten.mittenlib.tests;
                                                                                
                                        import java.util.Map;
                                                                                
                                        import me.bristermitten.mittenlib.config.*;
                                        import me.bristermitten.mittenlib.config.generate.GenerateToString;
                                        import me.bristermitten.mittenlib.config.names.*;
                                        import org.jetbrains.annotations.Nullable;
                                                                                
                                        @Config
                                        @NamingPattern(NamingPatterns.LOWER_KEBAB_CASE)
                                        @GenerateToString
                                        public class ShopConfigDTO {
                                            String title;
                                            int rows;
                                            Map<String, ShopItemConfigDTO> items;
                                                                                
                                                                                
                                            @Config
                                            static class ShopItemConfigDTO extends ItemConfigDTO {
                                                int medianStock;
                                                @Nullable Integer slot;
                                            }
                                        }"""),
                        JavaFileObjects.forSourceString("me.bristermitten.mittenlib.tests.ItemConfigDTO",
                                """
                                        package me.bristermitten.mittenlib.tests;
                                                                                
                                        import java.util.List;
                                        import java.util.Map;
                                                                                
                                        import me.bristermitten.mittenlib.config.*;
                                        import me.bristermitten.mittenlib.config.names.*;
                                        import org.bukkit.Material;
                                        import org.bukkit.enchantments.Enchantment;
                                        import org.bukkit.inventory.ItemFlag;
                                        import org.jetbrains.annotations.Nullable;
                                                                                
                                        @Config
                                        @NamingPattern(NamingPatterns.LOWER_KEBAB_CASE)
                                        public class ItemConfigDTO {
                                            Material type;
                                            @Nullable String name;
                                            @Nullable List<String> lore;
                                                                                
                                            @Nullable String player;
                                                                                
                                            boolean glow = false;
                                                                                
                                            @Nullable String dyeColor;
                                                                                
                                            @Nullable List<ItemFlag> flags;
                                                                                
                                            @Nullable List<EnchantmentDTO> enchantments;
                                                                                
                                            boolean unbreakable = false;
                                                                                
                                            @Config
                                            static class EnchantmentDTO {
                                                Enchantment type;
                                                int level = 1;
                                            }
                                        }
                                        """));

        assertThat(compilation).succeededWithoutWarnings();

        assertThat(compilation).generatedSourceFile("me.bristermitten.mittenlib.tests.ShopConfig")
                .isNotNull();
        // TODO figure out a proper way of testing contents -- AST comparison doesn't work as of java 17

    }
}

