package me.bristermitten.mittenlib.annotations.integration;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import me.bristermitten.mittenlib.MittenLibConsumer;
import me.bristermitten.mittenlib.config.ConfigModule;
import me.bristermitten.mittenlib.config.Configuration;
import me.bristermitten.mittenlib.config.provider.construct.ConfigProviderFactory;
import me.bristermitten.mittenlib.files.FileTypeModule;
import me.bristermitten.mittenlib.files.yaml.YamlFileType;
import me.bristermitten.mittenlib.watcher.FileWatcherModule;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static me.bristermitten.mittenlib.annotations.util.IntegrationTests.loadResourceString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatList;

public class IntegrationTest {

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
    void testInterfaceConfig() throws IOException {
        var fileContents = loadResourceString("integration/InterfaceConfig_dummy.yml");

        var stringReaderProvider = injector.getInstance(ConfigProviderFactory.class)
                .createStringReaderProvider(injector.getInstance(YamlFileType.class),
                        fileContents,
                        new Configuration<>(null, InterfaceConfig.class, InterfaceConfigImpl::deserializeInterfaceConfigImpl)
                ).getOrThrow();

        InterfaceConfig interfaceConfig = stringReaderProvider.get();

        assertThat(interfaceConfig).isNotNull();
        assertThat(interfaceConfig.name()).isEqualTo("a");
        assertThatList(interfaceConfig.children())
                .first()
                .isEqualTo(new InterfaceConfigImpl(
                        "b",
                        4,
                        List.of(
                                new InterfaceConfigImpl(
                                        "c", 5, List.of(), null
                                )
                        ), null
                ));

        assertThat(interfaceConfig.child())
                .isNotNull()
                .extracting(InterfaceConfig.ChildConfig::id)
                .isEqualTo("pee");
    }

    @Test
    void testClassConfig() throws IOException {
        var fileContents = loadResourceString("integration/InterfaceConfig_dummy.yml");

        var stringReaderProvider = injector.getInstance(ConfigProviderFactory.class)
                .createStringReaderProvider(injector.getInstance(YamlFileType.class),
                        fileContents,
                        new Configuration<>(null, ClassConfigImpl.class, ClassConfigImpl::deserializeClassConfigImpl)
                ).getOrThrow();

        ClassConfigImpl classConfig = stringReaderProvider.get();

        assertThat(classConfig).isNotNull();
        assertThat(classConfig.name()).isEqualTo("a");
        assertThatList(classConfig.children())
                .first()
                .isEqualTo(new InterfaceConfigImpl(
                        "b",
                        4,
                        List.of(
                                new InterfaceConfigImpl(
                                        "c", 5, List.of(), null
                                )
                        ), null
                ));

        assertThat(classConfig.child())
                .isNotNull()
                .extracting(ClassConfig.ChildConfig::id)
                .isEqualTo("pee");

        assertThat(classConfig.defaultValue())
                .isEqualTo(1);
    }

    @Test
    void testClassConfigIdenticalToInterface() throws IOException {
        var fileContents = loadResourceString("integration/InterfaceConfig_dummy.yml");

        var classStringReaderProvider = injector.getInstance(ConfigProviderFactory.class)
                .createStringReaderProvider(injector.getInstance(YamlFileType.class),
                        fileContents,
                        new Configuration<>(null, ClassConfigImpl.class, ClassConfigImpl::deserializeClassConfigImpl)
                ).getOrThrow();


        var interfaceStringReaderProvider = injector.getInstance(ConfigProviderFactory.class)
                .createStringReaderProvider(injector.getInstance(YamlFileType.class),
                        fileContents,
                        new Configuration<>(null, InterfaceConfig.class, InterfaceConfigImpl::deserializeInterfaceConfigImpl)
                ).getOrThrow();

        InterfaceConfig interfaceConfig = interfaceStringReaderProvider.get();
        ClassConfigImpl config = classStringReaderProvider.get();

        // assert that all fields with the same name are equal
        assertThat(interfaceConfig.name()).isEqualTo(config.name());
        assertThat(interfaceConfig.age()).isEqualTo(config.age());
        assertThat(interfaceConfig.children())
                .usingRecursiveComparison()
                .withEqualsForFields(
                        (InterfaceConfig.ChildConfig a, ClassConfigImpl.ChildConfigImpl b) -> a.id().equals(b.id())
                )
                .isEqualTo(config.children());
        assertThat(interfaceConfig.child())
                .isNotNull()
                .usingRecursiveComparison()
                // this is probably incomplete but i dont care that much ngl
                .isEqualTo(config.child());


    }

    @Test
    void testIntersectionConfigParsing() throws IOException {
        var fileContents = loadResourceString("integration/IntersectionConfig_1.yml");

        IntersectionConfig intersectionConfig = injector.getInstance(ConfigProviderFactory.class)
                .createStringReaderProvider(injector.getInstance(YamlFileType.class),
                        fileContents,
                        new Configuration<>(null, IntersectionConfig.class, IntersectionConfigImpl::deserializeIntersectionConfigImpl)
                ).getOrThrow()
                .get();

        assertThat(intersectionConfig).isNotNull();
        assertThat(intersectionConfig)
                .extracting(IntersectionConfig::base)
                .isEqualTo("hello");


        var fileContents2 = loadResourceString("integration/IntersectionConfig_2.yml");
        IntersectionConfig intersectionConfig2 = injector.getInstance(ConfigProviderFactory.class)
                .createStringReaderProvider(injector.getInstance(YamlFileType.class),
                        fileContents2,
                        new Configuration<>(null, IntersectionConfig.ChildIntersectionConfig.class, IntersectionConfigImpl.ChildIntersectionConfigImpl::deserializeChildIntersectionConfigImpl)
                ).getOrThrow()
                .get();

        assertThat(intersectionConfig2).isNotNull()
                .asInstanceOf(InstanceOfAssertFactories.type(IntersectionConfig.ChildIntersectionConfig.class))
                .extracting(IntersectionConfig.ChildIntersectionConfig::extra)
                .isEqualTo("wow");

        assertThat(intersectionConfig2)
                .extracting(IntersectionConfig::base)
                .isEqualTo("hello");
    }

    @Test
    void testUnionConfigParsing() throws IOException {
        var fileContents = loadResourceString("integration/UnionConfig_dummy.yml");

        var classStringReaderProvider = injector.getInstance(ConfigProviderFactory.class)
                .createStringReaderProvider(injector.getInstance(YamlFileType.class),
                        fileContents,
                        new Configuration<>(null, UnionConfig.class, UnionConfigImpl::deserializeUnionConfigImpl)
                ).getOrThrow();

        UnionConfig unionConfig = classStringReaderProvider.get();

        assertThat(unionConfig).isNotNull();

        assertThat(unionConfig)
                .asInstanceOf(InstanceOfAssertFactories.type(UnionConfig.Child1Config.class))
                .extracting(UnionConfig.Child1Config::hello)
                .isEqualTo("hi");
    }


}
