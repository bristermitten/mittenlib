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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;

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
    void test() throws IOException {
        var fileContents = new String(getClass().getClassLoader().getResourceAsStream("integration/InterfaceConfig_dummy.yml")
                .readAllBytes());

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


}
