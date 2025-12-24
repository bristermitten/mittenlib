package me.bristermitten.mittenlib.annotations.integration;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import me.bristermitten.mittenlib.MittenLibConsumer;
import me.bristermitten.mittenlib.config.ConfigModule;
import me.bristermitten.mittenlib.config.Configuration;
import me.bristermitten.mittenlib.config.provider.construct.ConfigProviderFactory;
import me.bristermitten.mittenlib.config.tree.DataTree;
import me.bristermitten.mittenlib.files.FileTypeModule;
import me.bristermitten.mittenlib.files.yaml.YamlFileType;
import me.bristermitten.mittenlib.watcher.FileWatcherModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;

import static me.bristermitten.mittenlib.annotations.util.IntegrationTests.loadResourceString;
import static org.assertj.core.api.Assertions.assertThat;

public class SaveDefaultsIntegrationTest {

    private Injector injector;

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
}
