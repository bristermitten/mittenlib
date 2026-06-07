package me.bristermitten.mittenlib.annotations.integration.extension;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Types;
import me.bristermitten.mittenlib.MittenLibConsumer;
import me.bristermitten.mittenlib.config.Configuration;
import me.bristermitten.mittenlib.config.SerializationContext;
import me.bristermitten.mittenlib.config.SerializationFunction;
import me.bristermitten.mittenlib.config.tree.DataTree;
import me.bristermitten.mittenlib.config.reader.ObjectMapper;
import me.bristermitten.mittenlib.files.FileTypeModule;
import me.bristermitten.mittenlib.files.yaml.YamlFileType;
import me.bristermitten.mittenlib.watcher.FileWatcherModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import me.bristermitten.mittenlib.config.DeserializationFunction;
import me.bristermitten.mittenlib.config.provider.construct.ConfigProviderFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomSerializerIntegrationTest {

    private Injector injector;

    @BeforeEach
    void setup() {
        injector = Guice.createInjector(
                new ConfigLoaderModule(),
                new FileWatcherModule(),
                new FileTypeModule(),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(MittenLibConsumer.class)
                                .toInstance(new MittenLibConsumer("SerializerTests"));
                    }
                }
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSerialization() {
        SerializationFunction<SerializerCustomTypeConfig> saver = (SerializationFunction<SerializerCustomTypeConfig>) injector.getInstance(
                Key.get(TypeLiteral.get(Types.newParameterizedType(SerializationFunction.class, SerializerCustomTypeConfig.class)))
        );

        SerializerCustomTypeConfig config = new SerializerCustomTypeConfig() {
            @Override
            public SerializerCustomType customType() {
                return new SerializerCustomType("test-value");
            }

            @Override
            public List<SerializerCustomType> customTypeList() {
                return List.of(
                        new SerializerCustomType("value-1"),
                        new SerializerCustomType("value-2")
                );
            }

            @Override
            public List<List<SerializerCustomType>> nestedCustomTypeList() {
                return List.of(
                        List.of(new SerializerCustomType("nested-1")),
                        List.of(new SerializerCustomType("nested-2"), new SerializerCustomType("nested-3"))
                );
            }
        };

        ObjectMapper mapper = injector.getInstance(ObjectMapper.class);
        SerializationContext context = new SerializationContext(mapper);
        DataTree result = saver.apply(config, context);

        assertThat(result).isNotNull();
        assertThat(result.get("customType")).isEqualTo(DataTree.string("serialized-test-value"));
        
        assertThat(result.get("customTypeList")).isInstanceOf(DataTree.DataTreeArray.class);
        assertThat(((DataTree.DataTreeArray) result.get("customTypeList")).value())
                .containsExactly(
                        DataTree.string("serialized-value-1"),
                        DataTree.string("serialized-value-2")
                );

        assertThat(result.get("nestedCustomTypeList")).isInstanceOf(DataTree.DataTreeArray.class);
        var outerArray = (DataTree.DataTreeArray) result.get("nestedCustomTypeList");
        assertThat(outerArray.value()).hasSize(2);
        
        var innerArray1 = (DataTree.DataTreeArray) outerArray.value().get(0);
        assertThat(innerArray1.value()).containsExactly(DataTree.string("serialized-nested-1"));

        var innerArray2 = (DataTree.DataTreeArray) outerArray.value().get(1);
        assertThat(innerArray2.value()).containsExactly(
                DataTree.string("serialized-nested-2"),
                DataTree.string("serialized-nested-3")
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void testDeserialization() {
        var stringReaderProvider = injector.getInstance(ConfigProviderFactory.class)
                .createStringReaderProvider(injector.getInstance(YamlFileType.class),
                        """
                        customType: 'anything'
                        customTypeList: ['anything1', 'anything2']
                        nestedCustomTypeList: [['anything3'], ['anything4', 'anything5']]
                        """,
                        new Configuration<>(null, SerializerCustomTypeConfig.class),
                        (DeserializationFunction<SerializerCustomTypeConfig>) injector.getInstance(Key.get(TypeLiteral.get(Types.newParameterizedType(DeserializationFunction.class, SerializerCustomTypeConfig.class)))),
                        (SerializationFunction<SerializerCustomTypeConfig>) injector.getInstance(Key.get(TypeLiteral.get(Types.newParameterizedType(SerializationFunction.class, SerializerCustomTypeConfig.class))))
                ).getOrThrow();

        SerializerCustomTypeConfig config = stringReaderProvider.get();
        assertThat(config).isNotNull();
        assertThat(config.customType()).isEqualTo(new SerializerCustomType("deserialized"));
        assertThat(config.customTypeList()).containsExactly(
                new SerializerCustomType("deserialized"),
                new SerializerCustomType("deserialized")
        );
        assertThat(config.nestedCustomTypeList()).containsExactly(
                List.of(new SerializerCustomType("deserialized")),
                List.of(new SerializerCustomType("deserialized"), new SerializerCustomType("deserialized"))
        );
    }
}
