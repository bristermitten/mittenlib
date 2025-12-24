package me.bristermitten.mittenlib.annotations.integration;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import me.bristermitten.mittenlib.MittenLibConsumer;
import me.bristermitten.mittenlib.config.ConfigModule;
import me.bristermitten.mittenlib.config.Configuration;
import me.bristermitten.mittenlib.config.provider.ConfigProvider;
import me.bristermitten.mittenlib.config.provider.ReadingConfigProvider;
import me.bristermitten.mittenlib.config.provider.construct.ConfigProviderFactory;
import me.bristermitten.mittenlib.config.reader.ConfigReader;
import me.bristermitten.mittenlib.config.tree.DataTree;
import me.bristermitten.mittenlib.files.FileTypeModule;
import me.bristermitten.mittenlib.files.yaml.YamlFileType;
import me.bristermitten.mittenlib.files.yaml.YamlObjectWriter;
import me.bristermitten.mittenlib.watcher.FileWatcherModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static me.bristermitten.mittenlib.annotations.util.IntegrationTests.loadResourceString;
import static org.assertj.core.api.Assertions.assertThat;

public class SaveDefaultsIntegrationTest {

    private Injector injector;
    
    @TempDir
    Path tempDir;

    @BeforeEach
    void setup() {
        injector = Guice.createInjector(
                new ConfigModule(Set.of()),
                new FileWatcherModule(),
                new FileTypeModule(),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(MittenLibConsumer.class)
                                .toInstance(new MittenLibConsumer("Tests"));
                    }
                }
        );
    }

    @Test
    void testSerializeClassConfig() throws IOException {
        var fileContents = loadResourceString("integration/InterfaceConfig_dummy.yml");

        var stringReaderProvider = injector.getInstance(ConfigProviderFactory.class)
                .createStringReaderProvider(injector.getInstance(YamlFileType.class),
                        fileContents,
                        new Configuration<>(null, ClassConfigImpl.class, ClassConfigImpl::deserializeClassConfigImpl, ClassConfigImpl::serializeClassConfigImpl)
                ).getOrThrow();

        ClassConfigImpl classConfig = (ClassConfigImpl) stringReaderProvider.get();

        // Verify default value was applied (defaultValue field is not in the YAML file)
        assertThat(classConfig.defaultValue()).isEqualTo(1);

        // Serialize the config back to a DataTree
        DataTree serialized = ClassConfigImpl.serializeClassConfigImpl(classConfig);
        
        // Verify the serialized data contains all fields including the default
        assertThat(serialized).isInstanceOf(DataTree.DataTreeMap.class);
        DataTree.DataTreeMap map = (DataTree.DataTreeMap) serialized;
        
        // Check that the defaultValue field is present in serialized form
        DataTree defaultValueTree = map.get("defaultValue");
        assertThat(defaultValueTree).isNotNull();
        assertThat(defaultValueTree).isInstanceOf(DataTree.DataTreeLiteral.DataTreeLiteralInt.class);
        assertThat(((DataTree.DataTreeLiteral.DataTreeLiteralInt) defaultValueTree).value).isEqualTo(1);
    }

    @Test
    void testSaveOnlyMissingFields() throws IOException {
        // Create a config file without the defaultValue field
        String originalContent = """
                age: 3
                thing-name: a
                children: []
                """;
        
        Path configFile = tempDir.resolve("test-config.yml");
        Files.writeString(configFile, originalContent);

        // Create a ReadingConfigProvider
        ConfigReader reader = injector.getInstance(ConfigReader.class);
        YamlObjectWriter writer = injector.getInstance(YamlObjectWriter.class);
        Configuration<ClassConfigImpl> config = new Configuration<>(
                configFile.getFileName().toString(),
                ClassConfigImpl.class,
                ClassConfigImpl::deserializeClassConfigImpl,
                ClassConfigImpl::serializeClassConfigImpl
        );
        ReadingConfigProvider<ClassConfigImpl> provider = new ReadingConfigProvider<>(configFile, config, reader, writer);

        // Load the config - defaultValue should be 1 (from the default)
        ClassConfigImpl classConfig = provider.get();
        assertThat(classConfig.defaultValue()).isEqualTo(1);
        assertThat(classConfig.age()).isEqualTo(3);
        assertThat(classConfig.name()).isEqualTo("a");

        // Save with default behavior (only add missing fields)
        provider.save(classConfig).getOrThrow();

        // Read the file back
        String savedContent = Files.readString(configFile);
        
        // Verify that the file now contains defaultValue
        assertThat(savedContent).contains("defaultValue: 1");
        // Verify existing fields are still present
        assertThat(savedContent).contains("age: 3");
        assertThat(savedContent).contains("thing-name: a");
    }

    @Test
    void testSaveWithOverride() throws IOException {
        // Create a config file with a different age value
        String originalContent = """
                age: 5
                thing-name: original
                children: []
                """;
        
        Path configFile = tempDir.resolve("test-config-override.yml");
        Files.writeString(configFile, originalContent);

        ConfigReader reader = injector.getInstance(ConfigReader.class);
        YamlObjectWriter writer = injector.getInstance(YamlObjectWriter.class);
        Configuration<ClassConfigImpl> config = new Configuration<>(
                configFile.getFileName().toString(),
                ClassConfigImpl.class,
                ClassConfigImpl::deserializeClassConfigImpl,
                ClassConfigImpl::serializeClassConfigImpl
        );
        ReadingConfigProvider<ClassConfigImpl> provider = new ReadingConfigProvider<>(configFile, config, reader, writer);

        // Load the config
        ClassConfigImpl classConfig = provider.get();
        assertThat(classConfig.age()).isEqualTo(5);
        
        // Modify the config in memory
        ClassConfigImpl modifiedConfig = classConfig.withAge(10).withName("modified");

        // Save with override = true (should replace the entire file)
        provider.save(modifiedConfig, true).getOrThrow();

        // Read the file back
        String savedContent = Files.readString(configFile);
        
        // Verify that the file has been completely overwritten
        assertThat(savedContent).contains("age: 10");
        assertThat(savedContent).contains("thing-name: modified");
        assertThat(savedContent).contains("defaultValue: 1");
    }

    @Test
    void testSavePreservesExistingFields() throws IOException {
        // Create a config file with all fields including a non-default value
        String originalContent = """
                age: 7
                thing-name: existing
                defaultValue: 99
                children: []
                """;
        
        Path configFile = tempDir.resolve("test-config-preserve.yml");
        Files.writeString(configFile, originalContent);

        ConfigReader reader = injector.getInstance(ConfigReader.class);
        YamlObjectWriter writer = injector.getInstance(YamlObjectWriter.class);
        Configuration<ClassConfigImpl> config = new Configuration<>(
                configFile.getFileName().toString(),
                ClassConfigImpl.class,
                ClassConfigImpl::deserializeClassConfigImpl,
                ClassConfigImpl::serializeClassConfigImpl
        );
        ReadingConfigProvider<ClassConfigImpl> provider = new ReadingConfigProvider<>(configFile, config, reader, writer);

        // Load the config - it has defaultValue = 99 (not the default 1)
        ClassConfigImpl classConfig = provider.get();
        assertThat(classConfig.defaultValue()).isEqualTo(99);

        // Save without override (should not change existing fields)
        provider.save(classConfig, false).getOrThrow();

        // Read the file back
        String savedContent = Files.readString(configFile);
        
        // Verify that existing values are preserved
        assertThat(savedContent).contains("defaultValue: 99");
        assertThat(savedContent).contains("age: 7");
        assertThat(savedContent).contains("thing-name: existing");
    }

    @Test
    void testSaveCreatesNewFileIfNotExists() throws IOException {
        // Path to a non-existent file
        Path configFile = tempDir.resolve("new-config.yml");
        assertThat(configFile).doesNotExist();

        ConfigReader reader = injector.getInstance(ConfigReader.class);
        YamlObjectWriter writer = injector.getInstance(YamlObjectWriter.class);
        Configuration<ClassConfigImpl> config = new Configuration<>(
                configFile.getFileName().toString(),
                ClassConfigImpl.class,
                ClassConfigImpl::deserializeClassConfigImpl,
                ClassConfigImpl::serializeClassConfigImpl
        );
        ReadingConfigProvider<ClassConfigImpl> provider = new ReadingConfigProvider<>(configFile, config, reader, writer);

        // Create a config instance manually
        ClassConfigImpl classConfig = new ClassConfigImpl("test", 42, 1, java.util.List.of(), null);

        // Save (should create the file since it doesn't exist)
        provider.save(classConfig, false).getOrThrow();

        // Verify the file was created
        assertThat(configFile).exists();
        
        String savedContent = Files.readString(configFile);
        assertThat(savedContent).contains("age: 42");
        assertThat(savedContent).contains("thing-name: test");
        assertThat(savedContent).contains("defaultValue: 1");
    }
}
