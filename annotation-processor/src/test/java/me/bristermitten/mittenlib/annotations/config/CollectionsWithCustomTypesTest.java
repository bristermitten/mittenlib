package me.bristermitten.mittenlib.annotations.config;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

/**
 * Tests the deserialization of collections with custom types in configuration classes.
 */
class CollectionsWithCustomTypesTest {

    @Test
    void testListWithCustomType() {
        Compilation compilation = javac()
                .withProcessors(new ConfigProcessor())
                .compile(JavaFileObjects.forSourceString("me.bristermitten.mittenlib.tests.ListWithCustomTypeConfigDTO",
                        """
                                package me.bristermitten.mittenlib.tests;
                                
                                import me.bristermitten.mittenlib.config.Config;
                                import me.bristermitten.mittenlib.config.Source;
                                import me.bristermitten.mittenlib.config.names.NamingPattern;
                                import me.bristermitten.mittenlib.config.names.NamingPatterns;
                                
                                import java.util.List;
                                
                                @NamingPattern(NamingPatterns.LOWER_KEBAB_CASE)
                                @Source("list_custom.yml")
                                @Config
                                public class ListWithCustomTypeConfigDTO {
                                    // List of custom config objects
                                    public List<PlayerConfigDTO> players;
                                
                                    @Config
                                    public static class PlayerConfigDTO {
                                        public String name;
                                        public int level = 1;
                                        public double health = 20.0;
                                    }
                                }
                                """));

        assertThat(compilation).succeededWithoutWarnings();
        
        // Verify that the generated class exists
        assertThat(compilation).generatedSourceFile("me.bristermitten.mittenlib.tests.ListWithCustomTypeConfig")
                .isNotNull();
                
        // Verify that the generated code includes list deserialization
        assertThat(compilation)
                .generatedSourceFile("me.bristermitten.mittenlib.tests.ListWithCustomTypeConfig")
                .contentsAsUtf8String()
                .contains("CollectionsUtils.deserializeList");
    }
    
    @Test
    void testMapWithCustomType() {
        Compilation compilation = javac()
                .withProcessors(new ConfigProcessor())
                .compile(JavaFileObjects.forSourceString("me.bristermitten.mittenlib.tests.MapWithCustomTypeConfigDTO",
                        """
                                package me.bristermitten.mittenlib.tests;
                                
                                import me.bristermitten.mittenlib.config.Config;
                                import me.bristermitten.mittenlib.config.Source;
                                import me.bristermitten.mittenlib.config.names.NamingPattern;
                                import me.bristermitten.mittenlib.config.names.NamingPatterns;
                                
                                import java.util.Map;
                                
                                @NamingPattern(NamingPatterns.LOWER_KEBAB_CASE)
                                @Source("map_custom.yml")
                                @Config
                                public class MapWithCustomTypeConfigDTO {
                                    // Map with string keys and custom config objects as values
                                    public Map<String, RegionConfigDTO> regions;

                                    @Config
                                    public static class RegionConfigDTO {
                                        public String name;
                                        public int minLevel = 1;
                                        public boolean pvpEnabled = false;
                                    }
                                }
                                """));

        assertThat(compilation).succeededWithoutWarnings();
        
        // Verify that the generated class exists
        assertThat(compilation).generatedSourceFile("me.bristermitten.mittenlib.tests.MapWithCustomTypeConfig")
                .isNotNull();
                
        // Verify that the generated code includes map deserialization
        assertThat(compilation)
                .generatedSourceFile("me.bristermitten.mittenlib.tests.MapWithCustomTypeConfig")
                .contentsAsUtf8String()
                .contains("CollectionsUtils.deserializeMap");
    }
    
    @Test
    void testNestedCollections() {
        Compilation compilation = javac()
                .withProcessors(new ConfigProcessor())
                .compile(JavaFileObjects.forSourceString("me.bristermitten.mittenlib.tests.NestedCollectionsConfigDTO",
                        """
                                package me.bristermitten.mittenlib.tests;
                                
                                import me.bristermitten.mittenlib.config.Config;
                                import me.bristermitten.mittenlib.config.Source;
                                import me.bristermitten.mittenlib.config.names.NamingPattern;
                                import me.bristermitten.mittenlib.config.names.NamingPatterns;
                                
                                import java.util.List;
                                import java.util.Map;
                                
                                @NamingPattern(NamingPatterns.LOWER_KEBAB_CASE)
                                @Source("nested_collections.yml")
                                @Config
                                public class NestedCollectionsConfigDTO {
                                    // Map with string keys and lists of custom config objects as values
                                    public Map<String, List<QuestConfigDTO>> questsByCategory;
                                
                                    @Config
                                    public static class QuestConfigDTO {
                                        public String id;
                                        public String name;
                                        public int difficulty = 1;
                                        public List<String> requirements;
                                        public Map<String, Integer> rewards;
                                    }
                                }
                                """));

        assertThat(compilation).succeededWithoutWarnings();
        
        // Verify that the generated class exists
        assertThat(compilation).generatedSourceFile("me.bristermitten.mittenlib.tests.NestedCollectionsConfig")
                .isNotNull();
    }
}