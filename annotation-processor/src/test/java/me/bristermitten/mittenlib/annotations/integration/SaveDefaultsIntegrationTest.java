package me.bristermitten.mittenlib.annotations.integration;

import static me.bristermitten.mittenlib.annotations.util.IntegrationTests.loadResourceString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.inject.*;
import com.google.inject.util.Types;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import me.bristermitten.mittenlib.MittenLibConsumer;
import me.bristermitten.mittenlib.config.Configuration;
import me.bristermitten.mittenlib.config.DeserializationFunction;
import me.bristermitten.mittenlib.config.SerializationContext;
import me.bristermitten.mittenlib.config.SerializationFunction;
import me.bristermitten.mittenlib.config.paths.PluginConfigInitializationStrategy;
import me.bristermitten.mittenlib.config.provider.FileBasedConfigProvider;
import me.bristermitten.mittenlib.config.provider.construct.ConfigProviderFactory;
import me.bristermitten.mittenlib.config.reader.ConfigReader;
import me.bristermitten.mittenlib.config.reader.ObjectMapper;
import me.bristermitten.mittenlib.config.tree.DataTree;
import me.bristermitten.mittenlib.config.writer.ConfigWriter;
import me.bristermitten.mittenlib.files.FileTypeModule;
import me.bristermitten.mittenlib.files.yaml.YamlFileType;
import me.bristermitten.mittenlib.files.yaml.YamlObjectWriter;
import me.bristermitten.mittenlib.util.Result;
import me.bristermitten.mittenlib.util.Unit;
import me.bristermitten.mittenlib.watcher.FileWatcherModule;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@SuppressWarnings("unchecked")
public class SaveDefaultsIntegrationTest {

    private final Plugin mockPlugin = mock(Plugin.class);

    @TempDir
    Path tempDir;

    private Injector injector;

    @BeforeEach
    void setup() {
        when(mockPlugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(mockPlugin.getName()).thenReturn("TestPlugin");

        injector = Guice.createInjector(
                new ConfigLoaderModule().asModuleWithInfrastructure(),
                new FileWatcherModule(),
                new FileTypeModule(),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(Plugin.class).toInstance(mockPlugin);
                        bind(MittenLibConsumer.class).toInstance(new MittenLibConsumer("Tests"));
                    }
                });
    }

    @Test
    void testSerializeClassConfig() throws IOException {
        var fileContents = loadResourceString("integration/InterfaceConfig_dummy.yml");

        DeserializationFunction<ClassConfigImpl> loader =
                (DeserializationFunction<ClassConfigImpl>) injector.getInstance(Key.get(TypeLiteral.get(
                        Types.newParameterizedType(DeserializationFunction.class, ClassConfigImpl.class))));
        SerializationFunction<ClassConfigImpl> saverFunc;
        saverFunc = (SerializationFunction<ClassConfigImpl>) injector.getInstance(Key.get(
                TypeLiteral.get(Types.newParameterizedType(SerializationFunction.class, ClassConfigImpl.class))));

        var stringReaderProvider = injector.getInstance(ConfigProviderFactory.class)
                .createStringReaderProvider(
                        injector.getInstance(YamlFileType.class),
                        fileContents,
                        new Configuration<>(null, ClassConfigImpl.class),
                        loader,
                        saverFunc)
                .getOrThrow();

        ClassConfigImpl classConfig = stringReaderProvider.get();

        // Verify default value was applied (defaultValue field is not in the YAML file)
        assertThat(classConfig.defaultValue()).isEqualTo(1);

        // Serialize the config back to a DataTree
        SerializationFunction<ClassConfigImpl> saver =
                (SerializationFunction<ClassConfigImpl>) injector.getInstance(Key.get(TypeLiteral.get(
                        Types.newParameterizedType(SerializationFunction.class, ClassConfigImpl.class))));
        DataTree serialized =
                saver.apply(classConfig, new SerializationContext(injector.getInstance(ObjectMapper.class)));

        // Verify the serialized data contains all fields including the default
        assertThat(serialized).isInstanceOf(DataTree.DataTreeMap.class);
        DataTree.DataTreeMap map = (DataTree.DataTreeMap) serialized;

        // Check that the defaultValue field is present in serialized form
        DataTree defaultValueTree = map.get("defaultValue");
        assertThat(defaultValueTree).isNotNull();
        assertThat(defaultValueTree).isInstanceOf(DataTree.DataTreeLiteral.DataTreeLiteralInt.class);
        assertThat(((DataTree.DataTreeLiteral.DataTreeLiteralInt) defaultValueTree).value)
                .isEqualTo(1);
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
        ConfigWriter saver = injector.getInstance(ConfigWriter.class);
        DeserializationFunction<ClassConfigImpl> loader =
                (DeserializationFunction<ClassConfigImpl>) injector.getInstance(Key.get(TypeLiteral.get(
                        Types.newParameterizedType(DeserializationFunction.class, ClassConfigImpl.class))));
        SerializationFunction<ClassConfigImpl> saverFunc =
                (SerializationFunction<ClassConfigImpl>) injector.getInstance(Key.get(TypeLiteral.get(
                        Types.newParameterizedType(SerializationFunction.class, ClassConfigImpl.class))));

        FileBasedConfigProvider<ClassConfigImpl> provider =
                new FileBasedConfigProvider<>(configFile, reader, loader, saver, saverFunc, writer);

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
        ConfigWriter saver = injector.getInstance(ConfigWriter.class);
        DeserializationFunction<ClassConfigImpl> loader =
                (DeserializationFunction<ClassConfigImpl>) injector.getInstance(Key.get(TypeLiteral.get(
                        Types.newParameterizedType(DeserializationFunction.class, ClassConfigImpl.class))));
        SerializationFunction<ClassConfigImpl> saverFunc =
                (SerializationFunction<ClassConfigImpl>) injector.getInstance(Key.get(TypeLiteral.get(
                        Types.newParameterizedType(SerializationFunction.class, ClassConfigImpl.class))));
        FileBasedConfigProvider<ClassConfigImpl> provider =
                new FileBasedConfigProvider<>(configFile, reader, loader, saver, saverFunc, writer);

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
        ConfigWriter saver = injector.getInstance(ConfigWriter.class);
        DeserializationFunction<ClassConfigImpl> loader =
                (DeserializationFunction<ClassConfigImpl>) injector.getInstance(Key.get(TypeLiteral.get(
                        Types.newParameterizedType(DeserializationFunction.class, ClassConfigImpl.class))));
        SerializationFunction<ClassConfigImpl> saverFunc =
                (SerializationFunction<ClassConfigImpl>) injector.getInstance(Key.get(TypeLiteral.get(
                        Types.newParameterizedType(SerializationFunction.class, ClassConfigImpl.class))));
        FileBasedConfigProvider<ClassConfigImpl> provider =
                new FileBasedConfigProvider<>(configFile, reader, loader, saver, saverFunc, writer);

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
        ConfigWriter saver = injector.getInstance(ConfigWriter.class);
        DeserializationFunction<ClassConfigImpl> loader =
                (DeserializationFunction<ClassConfigImpl>) injector.getInstance(Key.get(TypeLiteral.get(
                        Types.newParameterizedType(DeserializationFunction.class, ClassConfigImpl.class))));
        SerializationFunction<ClassConfigImpl> saverFunc =
                (SerializationFunction<ClassConfigImpl>) injector.getInstance(Key.get(TypeLiteral.get(
                        Types.newParameterizedType(SerializationFunction.class, ClassConfigImpl.class))));

        FileBasedConfigProvider<ClassConfigImpl> provider =
                new FileBasedConfigProvider<>(configFile, reader, loader, saver, saverFunc, writer);

        // Create a config instance manually
        ClassConfigImpl classConfig = new ClassConfigImpl("test", 42, 1, List.of(), null);

        // Save (should create the file since it doesn't exist)
        provider.save(classConfig, false).getOrThrow();

        // Verify the file was created
        assertThat(configFile).exists();

        String savedContent = Files.readString(configFile);
        assertThat(savedContent).contains("age: 42");
        assertThat(savedContent).contains("thing-name: test");
        assertThat(savedContent).contains("defaultValue: 1");
    }

    @Test
    void testGenerateDefaultWhenFileMissing() throws IOException {
        // Path to a non-existent file
        Path configFile = tempDir.resolve("generated-default-config.yml");
        assertThat(configFile).doesNotExist();

        ConfigReader reader = injector.getInstance(ConfigReader.class);
        YamlObjectWriter writer = injector.getInstance(YamlObjectWriter.class);
        ConfigWriter saver = injector.getInstance(ConfigWriter.class);

        DeserializationFunction<FullyDefaultConfigImpl> loader =
                (DeserializationFunction<FullyDefaultConfigImpl>) injector.getInstance(Key.get(TypeLiteral.get(
                        Types.newParameterizedType(DeserializationFunction.class, FullyDefaultConfigImpl.class))));
        SerializationFunction<FullyDefaultConfigImpl> saverFunc =
                (SerializationFunction<FullyDefaultConfigImpl>) injector.getInstance(Key.get(TypeLiteral.get(
                        Types.newParameterizedType(SerializationFunction.class, FullyDefaultConfigImpl.class))));

        FileBasedConfigProvider<FullyDefaultConfigImpl> provider =
                new FileBasedConfigProvider<>(configFile, reader, loader, saver, saverFunc, writer);

        // Calling get() should succeed because FullyDefaultConfig is dynamically initializable
        FullyDefaultConfigImpl config = provider.get();

        // Verify the file was created automatically
        assertThat(configFile).exists();

        // Verify values
        assertThat(config.age()).isEqualTo(42);
        assertThat(config.name()).isEqualTo("default");

        String savedContent = Files.readString(configFile);
        assertThat(savedContent).contains("age: 42");
        assertThat(savedContent).contains("name: default");
    }

    @Test
    void testDenyMissingResourceForNonDynamicConfig() {
        // Path to a non-existent file
        Path configFile = tempDir.resolve("non-existent-config.yml");

        ConfigReader reader = injector.getInstance(ConfigReader.class);
        YamlObjectWriter writer = injector.getInstance(YamlObjectWriter.class);
        ConfigWriter saver = injector.getInstance(ConfigWriter.class);

        DeserializationFunction<ClassConfigImpl> loader =
                (DeserializationFunction<ClassConfigImpl>) injector.getInstance(Key.get(TypeLiteral.get(
                        Types.newParameterizedType(DeserializationFunction.class, ClassConfigImpl.class))));
        SerializationFunction<ClassConfigImpl> saverFunc =
                (SerializationFunction<ClassConfigImpl>) injector.getInstance(Key.get(TypeLiteral.get(
                        Types.newParameterizedType(SerializationFunction.class, ClassConfigImpl.class))));

        FileBasedConfigProvider<ClassConfigImpl> provider =
                new FileBasedConfigProvider<>(configFile, reader, loader, saver, saverFunc, writer);

        // Calling get() should fail with NoSuchFileException because ClassConfig is NOT dynamically
        // initializable
        // (it has required fields name and age without defaults)
        // and it is not found in the JAR, so it is not copied to the data folder.
        assertThatThrownBy(provider::get).isInstanceOf(NoSuchFileException.class);

        assertThat(configFile).doesNotExist();
    }

    @Test
    void testInformativeErrorMessageWhenDynamicInitializationFails() {
        PluginConfigInitializationStrategy strategy = injector.getInstance(PluginConfigInitializationStrategy.class);

        // ClassConfig is NOT dynamically initializable because of 'name' and 'age'
        Result<Unit> result = strategy.initializeConfig("non-existent-config.yml", ClassConfigImpl.class);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isPresent();
        assertThat(result.error().get().getMessage())
                .contains("Could not find resource non-existent-config.yml")
                .contains("is not dynamically initializable")
                .contains("following required properties lack default values: name, age, children")
                .contains("Either provide a default config file in your jar's resources");
    }
}
