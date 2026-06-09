package me.bristermitten.mittenlib.annotations.integration.extension;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.*;
import com.google.inject.util.Types;
import me.bristermitten.mittenlib.MittenLibConsumer;
import me.bristermitten.mittenlib.annotations.integration.ConfigLoaderModule;
import me.bristermitten.mittenlib.annotations.integration.extension.fallback.CustomTypeFallback;
import me.bristermitten.mittenlib.annotations.integration.extension.fallback.CustomTypeFallbackConfig;
import me.bristermitten.mittenlib.annotations.integration.extension.fallback.CustomTypeFallbackConfigImpl;
import me.bristermitten.mittenlib.config.ConfigInfrastructureModule;
import me.bristermitten.mittenlib.config.Configuration;
import me.bristermitten.mittenlib.config.DeserializationFunction;
import me.bristermitten.mittenlib.config.SerializationFunction;
import me.bristermitten.mittenlib.config.provider.construct.ConfigProviderFactory;
import me.bristermitten.mittenlib.files.FileTypeModule;
import me.bristermitten.mittenlib.files.yaml.YamlFileType;
import me.bristermitten.mittenlib.watcher.FileWatcherModule;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CustomDeserializerIntegrationTest {

    private Injector injector;

    @BeforeEach
    void setup() {
        injector = Guice.createInjector(
                new ConfigInfrastructureModule(),
                new ConfigLoaderModule().asModule(),
                new FileWatcherModule(),
                new FileTypeModule(),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(MittenLibConsumer.class).toInstance(new MittenLibConsumer("EnumTests"));
                    }
                });
    }

    @Test
    @SuppressWarnings("unchecked")
    void test() {
        var stringReaderProvider = injector.getInstance(ConfigProviderFactory.class)
                .createStringReaderProvider(
                        injector.getInstance(YamlFileType.class),
                        """
                                   customType: 'blahblah'
                                   customTypes: [ 'f' ]
                                """,
                        new Configuration<>(null, CustomTypeConfig.class, CustomTypeConfigImpl.class),
                        (DeserializationFunction<CustomTypeConfig>) injector.getInstance(Key.get(TypeLiteral.get(
                                Types.newParameterizedType(DeserializationFunction.class, CustomTypeConfig.class)))),
                        (SerializationFunction<CustomTypeConfig>) injector.getInstance(Key.get(TypeLiteral.get(
                                Types.newParameterizedType(SerializationFunction.class, CustomTypeConfig.class)))))
                .getOrThrow();

        CustomTypeConfig customTypeConfig = stringReaderProvider.get();
        assertThat(customTypeConfig).extracting(CustomTypeConfig::customType).isEqualTo(new CustomType("hello"));

        assertThat(customTypeConfig)
                .extracting(CustomTypeConfig::customTypes)
                .asInstanceOf(InstanceOfAssertFactories.list(CustomType.class))
                .singleElement()
                .isEqualTo(new CustomType("hello"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testFallback() {
        var stringReaderProvider = injector.getInstance(ConfigProviderFactory.class)
                .createStringReaderProvider(
                        injector.getInstance(YamlFileType.class),
                        "customType: { test: blahblah }",
                        new Configuration<>(null, CustomTypeFallbackConfig.class, CustomTypeFallbackConfigImpl.class),
                        (DeserializationFunction<CustomTypeFallbackConfig>)
                                injector.getInstance(Key.get(TypeLiteral.get(Types.newParameterizedType(
                                        DeserializationFunction.class, CustomTypeFallbackConfig.class)))),
                        (SerializationFunction<CustomTypeFallbackConfig>)
                                injector.getInstance(Key.get(TypeLiteral.get(Types.newParameterizedType(
                                        SerializationFunction.class, CustomTypeFallbackConfig.class)))))
                .getOrThrow();

        var customTypeConfig = stringReaderProvider.get();
        assertThat(customTypeConfig)
                .extracting(CustomTypeFallbackConfig::customType)
                .extracting(CustomTypeFallback::test)
                .isEqualTo("blahblah");
    }
}
