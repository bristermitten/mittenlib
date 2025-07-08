package me.bristermitten.mittenlib.annotations.integration.enums;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import me.bristermitten.mittenlib.MittenLibConsumer;
import me.bristermitten.mittenlib.config.ConfigModule;
import me.bristermitten.mittenlib.config.Configuration;
import me.bristermitten.mittenlib.config.exception.InvalidEnumValueException;
import me.bristermitten.mittenlib.config.provider.construct.ConfigProviderFactory;
import me.bristermitten.mittenlib.files.FileTypeModule;
import me.bristermitten.mittenlib.files.yaml.YamlFileType;
import me.bristermitten.mittenlib.watcher.FileWatcherModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class EnumIntegrationTest {

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
                                .toInstance(new MittenLibConsumer("EnumTests"));
                    }
                }
        );
    }

    private String loadResourceString(String resource) throws IOException {
        try (var res = getClass().getClassLoader().getResourceAsStream(resource)) {
            if (res == null) {
                throw new IOException("Resource not found: " + resource);
            }
            return new String(res.readAllBytes());
        }
    }

    @Test
    void testEnums() throws IOException {
        var fileContents = loadResourceString("integration/enums/TestEnumConfig_1.yml");

        var stringReaderProvider = injector.getInstance(ConfigProviderFactory.class)
                .createStringReaderProvider(injector.getInstance(YamlFileType.class),
                        fileContents,
                        new Configuration<>(null, TestEnumConfig.class, TestEnumConfigImpl::deserializeTestEnumConfigImpl)
                ).getOrThrow();

        TestEnumConfig interfaceConfig = stringReaderProvider.get();

        assertThat(interfaceConfig).isNotNull()
                .extracting(TestEnumConfig::testEnum)
                .isEqualTo(TestEnum.HELLO);

        assertThat(interfaceConfig)
                .extracting(TestEnumConfig::testEnumInexact)
                .isEqualTo(TestEnum.WORLD);
    }

    @Test
    void testEnumsCascading() throws IOException {
        var fileContents = loadResourceString("integration/enums/TestEnumConfig_1.yml");

        var stringReaderProvider = injector.getInstance(ConfigProviderFactory.class)
                .createStringReaderProvider(injector.getInstance(YamlFileType.class),
                        fileContents,
                        new Configuration<>(null, TestEnumCascadeConfig.class, TestEnumCascadeConfigImpl::deserializeTestEnumCascadeConfigImpl)
                ).getOrThrow();

        TestEnumCascadeConfig interfaceConfig = stringReaderProvider.get();

        assertThat(interfaceConfig).isNotNull()
                .extracting(TestEnumCascadeConfig::testEnum)
                .isEqualTo(TestEnum.HELLO);

        assertThat(interfaceConfig)
                .extracting(TestEnumCascadeConfig::testEnumInexact)
                .isEqualTo(TestEnum.WORLD);
    }

    @Test
    void testEnumsInvalid() throws IOException {
        var fileContents = loadResourceString("integration/enums/TestEnumConfig_invalid.yml");

        var stringReaderProvider = injector.getInstance(ConfigProviderFactory.class)
                .createStringReaderProvider(injector.getInstance(YamlFileType.class),
                        fileContents,
                        new Configuration<>(null, TestEnumConfig.class, TestEnumConfigImpl::deserializeTestEnumConfigImpl)
                ).getOrThrow();

        assertThatThrownBy(stringReaderProvider::get)
                .isInstanceOf(InvalidEnumValueException.class);

    }

}
