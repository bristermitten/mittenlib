package me.bristermitten.mittenlib.annotations.integration.enums;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Types;
import me.bristermitten.mittenlib.annotations.integration.ConfigLoaderModule;
import me.bristermitten.mittenlib.MittenLibConsumer;
import me.bristermitten.mittenlib.config.ConfigInfrastructureModule;
import me.bristermitten.mittenlib.config.Configuration;
import me.bristermitten.mittenlib.config.DeserializationFunction;
import me.bristermitten.mittenlib.config.SerializationFunction;
import me.bristermitten.mittenlib.config.exception.InvalidEnumValueException;
import me.bristermitten.mittenlib.config.provider.construct.ConfigProviderFactory;
import me.bristermitten.mittenlib.files.FileTypeModule;
import me.bristermitten.mittenlib.files.yaml.YamlFileType;
import me.bristermitten.mittenlib.watcher.FileWatcherModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;

import static me.bristermitten.mittenlib.annotations.util.IntegrationTests.loadResourceString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class EnumIntegrationTest {

    private Injector injector;

    @BeforeEach
    void setup() {
        injector = Guice.createInjector(
                new ConfigLoaderModule().asModuleWithInfrastructure(),
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
    void testEnums() throws IOException {
        var fileContents = loadResourceString("integration/enums/TestEnumConfig_1.yml");

        TestEnumConfig config = injector.getInstance(ConfigProviderFactory.class)
                .createStringReaderProvider(injector.getInstance(YamlFileType.class),
                        fileContents,
                        new Configuration<>(null, TestEnumConfig.class),
                        (DeserializationFunction<TestEnumConfig>) injector.getInstance(Key.get(TypeLiteral.get(Types.newParameterizedType(DeserializationFunction.class, TestEnumConfig.class)))),
                        (SerializationFunction<TestEnumConfig>) injector.getInstance(Key.get(TypeLiteral.get(Types.newParameterizedType(SerializationFunction.class, TestEnumConfig.class))))
                ).getOrThrow().get();


        assertThat(config).isNotNull()
                .extracting(TestEnumConfig::testEnum)
                .isEqualTo(TestEnum.HELLO);

        assertThat(config)
                .extracting(TestEnumConfig::testEnumInexact)
                .isEqualTo(TestEnum.WORLD);
    }

    @Test
    void testEnumsCascading() throws IOException {
        var fileContents = loadResourceString("integration/enums/TestEnumConfig_1.yml");

        TestEnumCascadeConfig config = injector.getInstance(ConfigProviderFactory.class)
                .createStringReaderProvider(injector.getInstance(YamlFileType.class),
                        fileContents,
                        new Configuration<>(null, TestEnumCascadeConfig.class),
                        (DeserializationFunction<TestEnumCascadeConfig>) injector.getInstance(Key.get(TypeLiteral.get(Types.newParameterizedType(DeserializationFunction.class, TestEnumCascadeConfig.class)))),
                        (SerializationFunction<TestEnumCascadeConfig>) injector.getInstance(Key.get(TypeLiteral.get(Types.newParameterizedType(SerializationFunction.class, TestEnumCascadeConfig.class))))
                ).getOrThrow().get();

        assertThat(config).isNotNull()
                .extracting(TestEnumCascadeConfig::testEnum)
                .isEqualTo(TestEnum.HELLO);
        
        assertThat(config)
                .extracting(TestEnumCascadeConfig::testEnumInexact)
                .isEqualTo(TestEnum.WORLD);
    }

    @Test
    void testEnumsInvalid() throws IOException {
        var fileContents = loadResourceString("integration/enums/TestEnumConfig_invalid.yml");

        var provider = injector.getInstance(ConfigProviderFactory.class)
                .createStringReaderProvider(injector.getInstance(YamlFileType.class),
                        fileContents,
                        new Configuration<>(null, TestEnumConfig.class),
                        (DeserializationFunction<TestEnumConfig>) injector.getInstance(Key.get(TypeLiteral.get(Types.newParameterizedType(DeserializationFunction.class, TestEnumConfig.class)))),
                        (SerializationFunction<TestEnumConfig>) injector.getInstance(Key.get(TypeLiteral.get(Types.newParameterizedType(SerializationFunction.class, TestEnumConfig.class))))
                ).getOrThrow();


        assertThatThrownBy(provider::get)
                .isInstanceOf(InvalidEnumValueException.class);

    }

}
