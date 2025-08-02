package me.bristermitten.mittenlib.annotations.config;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

/**
 * Tests the deserialization of complex nested structures in configuration classes.
 */
class ComplexNestedStructuresTest {

    @Test
    void testComplexNestedStructures() {
        Compilation compilation = javac()
                .withProcessors(new ConfigProcessor())
                .compile(JavaFileObjects.forSourceString("me.bristermitten.mittenlib.tests.GameConfigDTO",
                        """
                                package me.bristermitten.mittenlib.tests;

                                import me.bristermitten.mittenlib.config.Config;
                                import me.bristermitten.mittenlib.config.Source;
                                import me.bristermitten.mittenlib.config.names.NamingPattern;
                                import me.bristermitten.mittenlib.config.names.NamingPatterns;
                                import org.jspecify.annotations.Nullable;

                                import java.util.List;
                                import java.util.Map;

                                @NamingPattern(NamingPatterns.LOWER_KEBAB_CASE)
                                @Source("game.yml")
                                @Config
                                public class GameConfigDTO {
                                    // Game settings
                                    public String name = "Default Game";
                                    public int maxPlayers = 10;
                                    public boolean enableTeams = true;

                                    // Nested world configuration
                                    public WorldConfigDTO world;

                                    // Multiple game modes
                                    public Map<String, GameModeDTO> gameModes;

                                    // List of available items
                                    public List<ItemDTO> availableItems;

                                    // Nested classes
                                    @Config
                                    public static class WorldConfigDTO {
                                        public String name = "default_world";
                                        public int size = 1000;
                                        public boolean generateStructures = true;

                                        // Nested biome configuration
                                        public Map<String, BiomeDTO> biomes;

                                        // Nested spawn configuration
                                        public SpawnConfigDTO spawnConfig;

                                        @Config
                                        public static class BiomeDTO {
                                            public String name;
                                            public double temperature = 0.5;
                                            public double humidity = 0.5;
                                            public List<String> entities;
                                        }

                                        @Config
                                        public static class SpawnConfigDTO {
                                            public int x = 0;
                                            public int y = 64;
                                            public int z = 0;
                                            @Nullable public String message = null;
                                        }
                                    }

                                    @Config
                                    public static class GameModeDTO {
                                        public String name;
                                        public int duration = 600; // in seconds
                                        public List<String> allowedItems;
                                        public Map<String, Integer> scoreMultipliers;
                                    }

                                    @Config
                                    public static class ItemDTO {
                                        public String id;
                                        public String name;
                                        public int value = 1;
                                        public boolean tradeable = true;

                                        // Nested attributes
                                        public Map<String, Integer> attributes;

                                        // Nested effects
                                        @Nullable public List<EffectDTO> effects = null;

                                        @Config
                                        public static class EffectDTO {
                                            public String type;
                                            public int duration = 30;
                                            public int amplifier = 1;
                                        }
                                    }
                                }
                                """));

        assertThat(compilation).succeededWithoutWarnings();

        // Verify that the main generated class exists
        assertThat(compilation).generatedSourceFile("me.bristermitten.mittenlib.tests.GameConfig")
                .isNotNull();
    }
}
