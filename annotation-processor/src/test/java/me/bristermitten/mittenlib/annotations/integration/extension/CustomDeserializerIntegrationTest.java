package me.bristermitten.mittenlib.annotations.integration.extension;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import me.bristermitten.mittenlib.MittenLibConsumer;
import me.bristermitten.mittenlib.annotations.integration.extension.fallback.CustomTypeFallback;
import me.bristermitten.mittenlib.annotations.integration.extension.fallback.CustomTypeFallbackConfig;
import me.bristermitten.mittenlib.annotations.integration.extension.fallback.CustomTypeFallbackConfigImpl;
import me.bristermitten.mittenlib.config.ConfigModule;
import me.bristermitten.mittenlib.config.Configuration;
import me.bristermitten.mittenlib.config.provider.construct.ConfigProviderFactory;
import me.bristermitten.mittenlib.files.FileTypeModule;
import me.bristermitten.mittenlib.files.yaml.YamlFileType;
import me.bristermitten.mittenlib.watcher.FileWatcherModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;


public class CustomDeserializerIntegrationTest {

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

    @Test
    void test() {
        var stringReaderProvider = injector.getInstance(ConfigProviderFactory.class)
                .createStringReaderProvider(injector.getInstance(YamlFileType.class),
                        "customType: blahblah",
                        new Configuration<>(null, CustomTypeConfig.class, CustomTypeConfigImpl::deserializeCustomTypeConfigImpl)
                ).getOrThrow();

        CustomTypeConfig customTypeConfig = stringReaderProvider.get();
        assertThat(customTypeConfig)
                .extracting(CustomTypeConfig::customType)
                .isEqualTo(new CustomType("hello"));
    }

    @Test
    void testFallback() {
        var stringReaderProvider = injector.getInstance(ConfigProviderFactory.class)
                .createStringReaderProvider(injector.getInstance(YamlFileType.class),
                        "customType: { test: blahblah }",
                        new Configuration<>(null, CustomTypeFallbackConfig.class, CustomTypeFallbackConfigImpl::deserializeCustomTypeFallbackConfigImpl)
                ).getOrThrow();

        var customTypeConfig = stringReaderProvider.get();
        assertThat(customTypeConfig)
                .extracting(CustomTypeFallbackConfig::customType)
                .extracting(CustomTypeFallback::test)
                .isEqualTo("blahblah");

    }
}
