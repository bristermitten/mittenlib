package me.bristermitten.mittenlib.annotations.integration;

import static me.bristermitten.mittenlib.annotations.util.IntegrationTests.loadResourceString;
import static org.assertj.core.api.Assertions.*;

import com.google.inject.*;
import com.google.inject.util.Types;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import me.bristermitten.mittenlib.MittenLibConsumer;
import me.bristermitten.mittenlib.config.Configuration;
import me.bristermitten.mittenlib.config.DeserializationFunction;
import me.bristermitten.mittenlib.config.SerializationFunction;
import me.bristermitten.mittenlib.config.exception.ConfigValidationException;
import me.bristermitten.mittenlib.config.provider.construct.ConfigProviderFactory;
import me.bristermitten.mittenlib.files.FileTypeModule;
import me.bristermitten.mittenlib.files.yaml.YamlFileType;
import me.bristermitten.mittenlib.util.Result;
import me.bristermitten.mittenlib.watcher.FileWatcherModule;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
public class IntegrationTest {

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
                        bind(MittenLibConsumer.class).toInstance(new MittenLibConsumer("Tests"));
                    }
                });
    }

    @Test
    void testBindingExists() {
        var key = Key.get(
                TypeLiteral.get(Types.newParameterizedType(DeserializationFunction.class, InterfaceConfig.class)));
        var instance = injector.getInstance(key);
        assertThat(instance).isNotNull();
    }

    @Test
    void testInterfaceConfig() throws IOException {
        var fileContents = loadResourceString("integration/InterfaceConfig_dummy.yml");

        var stringReaderProvider = injector.getInstance(ConfigProviderFactory.class)
                .createStringReaderProvider(
                        injector.getInstance(YamlFileType.class),
                        fileContents,
                        new Configuration<>(null, InterfaceConfig.class, InterfaceConfigImpl.class),
                        (DeserializationFunction<InterfaceConfig>) injector.getInstance(Key.get(TypeLiteral.get(
                                Types.newParameterizedType(DeserializationFunction.class, InterfaceConfig.class)))),
                        (SerializationFunction<InterfaceConfig>) injector.getInstance(Key.get(TypeLiteral.get(
                                Types.newParameterizedType(SerializationFunction.class, InterfaceConfig.class)))))
                .getOrThrow();

        InterfaceConfig interfaceConfig = stringReaderProvider.get();

        assertThat(interfaceConfig).isNotNull();
        assertThat(interfaceConfig.name()).isEqualTo("a");
        assertThatList(interfaceConfig.children())
                .first()
                .isEqualTo(new InterfaceConfigImpl(
                        "b", 4, List.of(new InterfaceConfigImpl("c", 5, List.of(), null)), null));

        //noinspection DataFlowIssue intellij doesnt understand isNotNull()
        assertThat(interfaceConfig.child())
                .isNotNull()
                .extracting(InterfaceConfig.ChildConfig::id)
                .isEqualTo("pee");
    }

    @Test
    void testClassConfig() throws IOException {
        var fileContents = loadResourceString("integration/InterfaceConfig_dummy.yml");

        var stringReaderProvider = injector.getInstance(ConfigProviderFactory.class)
                .createStringReaderProvider(
                        injector.getInstance(YamlFileType.class),
                        fileContents,
                        new Configuration<>(null, ClassConfigImpl.class, ClassConfigImpl.class),
                        (DeserializationFunction<ClassConfigImpl>) injector.getInstance(Key.get(TypeLiteral.get(
                                Types.newParameterizedType(DeserializationFunction.class, ClassConfigImpl.class)))),
                        (SerializationFunction<ClassConfigImpl>) injector.getInstance(Key.get(TypeLiteral.get(
                                Types.newParameterizedType(SerializationFunction.class, ClassConfigImpl.class)))))
                .getOrThrow();

        ClassConfigImpl classConfig = stringReaderProvider.get();

        assertThat(classConfig).isNotNull();
        assertThat(classConfig.name()).isEqualTo("a");
        assertThatList(classConfig.children())
                .first()
                .isEqualTo(new InterfaceConfigImpl(
                        "b", 4, List.of(new InterfaceConfigImpl("c", 5, List.of(), null)), null));

        //noinspection DataFlowIssue
        assertThat(classConfig.child())
                .isNotNull()
                .extracting(ClassConfig.ChildConfig::id)
                .isEqualTo("pee");

        assertThat(classConfig.defaultValue()).isEqualTo(1);
    }

    @Test
    void testClassConfigIdenticalToInterface() throws IOException {
        var fileContents = loadResourceString("integration/InterfaceConfig_dummy.yml");

        var classStringReaderProvider = injector.getInstance(ConfigProviderFactory.class)
                .createStringReaderProvider(
                        injector.getInstance(YamlFileType.class),
                        fileContents,
                        new Configuration<>(null, ClassConfigImpl.class, ClassConfigImpl.class),
                        (DeserializationFunction<ClassConfigImpl>) injector.getInstance(Key.get(TypeLiteral.get(
                                Types.newParameterizedType(DeserializationFunction.class, ClassConfigImpl.class)))),
                        (SerializationFunction<ClassConfigImpl>) injector.getInstance(Key.get(TypeLiteral.get(
                                Types.newParameterizedType(SerializationFunction.class, ClassConfigImpl.class)))))
                .getOrThrow();

        var interfaceStringReaderProvider = injector.getInstance(ConfigProviderFactory.class)
                .createStringReaderProvider(
                        injector.getInstance(YamlFileType.class),
                        fileContents,
                        new Configuration<>(null, InterfaceConfig.class, InterfaceConfigImpl.class),
                        (DeserializationFunction<InterfaceConfig>) injector.getInstance(Key.get(TypeLiteral.get(
                                Types.newParameterizedType(DeserializationFunction.class, InterfaceConfig.class)))),
                        (SerializationFunction<InterfaceConfig>) injector.getInstance(Key.get(TypeLiteral.get(
                                Types.newParameterizedType(SerializationFunction.class, InterfaceConfig.class)))))
                .getOrThrow();

        InterfaceConfig interfaceConfig = interfaceStringReaderProvider.get();
        ClassConfigImpl config = classStringReaderProvider.get();

        // assert that all fields with the same name are equal
        assertThat(interfaceConfig.name()).isEqualTo(config.name());
        assertThat(interfaceConfig.age()).isEqualTo(config.age());
        assertThat(interfaceConfig.children())
                .usingRecursiveComparison()
                .withEqualsForFields(
                        (InterfaceConfig.ChildConfig a, ClassConfigImpl.ChildConfigImpl b) -> a.id().equals(b.id()))
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
                .createStringReaderProvider(
                        injector.getInstance(YamlFileType.class),
                        fileContents,
                        new Configuration<>(null, IntersectionConfig.class, IntersectionConfigImpl.class),
                        (DeserializationFunction<IntersectionConfig>) injector.getInstance(Key.get(TypeLiteral.get(
                                Types.newParameterizedType(DeserializationFunction.class, IntersectionConfig.class)))),
                        (SerializationFunction<IntersectionConfig>) injector.getInstance(Key.get(TypeLiteral.get(
                                Types.newParameterizedType(SerializationFunction.class, IntersectionConfig.class)))))
                .getOrThrow()
                .get();

        assertThat(intersectionConfig).isNotNull();
        assertThat(intersectionConfig).extracting(IntersectionConfig::base).isEqualTo("hello");
    }

    @Test
    void testIntersectionConfigParsingChild() throws IOException {
        var fileContents = loadResourceString("integration/IntersectionConfig_2.yml");

        IntersectionConfig intersectionConfig2 = injector.getInstance(ConfigProviderFactory.class)
                .createStringReaderProvider(
                        injector.getInstance(YamlFileType.class),
                        fileContents,
                        new Configuration<>(
                                null,
                                IntersectionConfig.ChildIntersectionConfig.class,
                                IntersectionConfigImpl.ChildIntersectionConfigImpl.class),
                        (DeserializationFunction<IntersectionConfig.ChildIntersectionConfig>)
                                injector.getInstance(Key.get(TypeLiteral.get(Types.newParameterizedType(
                                        DeserializationFunction.class,
                                        IntersectionConfig.ChildIntersectionConfig.class)))),
                        (SerializationFunction<IntersectionConfig.ChildIntersectionConfig>)
                                injector.getInstance(Key.get(TypeLiteral.get(Types.newParameterizedType(
                                        SerializationFunction.class,
                                        IntersectionConfig.ChildIntersectionConfig.class)))))
                .getOrThrow()
                .get();

        assertThat(intersectionConfig2)
                .isNotNull()
                .asInstanceOf(InstanceOfAssertFactories.type(IntersectionConfig.ChildIntersectionConfig.class))
                .extracting(IntersectionConfig.ChildIntersectionConfig::extra)
                .isEqualTo("wow");

        assertThat(intersectionConfig2).extracting(IntersectionConfig::base).isEqualTo("hello");
    }

    @Test
    void testUnionConfigParsing() throws IOException {
        var fileContents = loadResourceString("integration/UnionConfig_dummy.yml");

        var classStringReaderProvider = injector.getInstance(ConfigProviderFactory.class)
                .createStringReaderProvider(
                        injector.getInstance(YamlFileType.class),
                        fileContents,
                        new Configuration<>(null, UnionConfig.class, UnionConfigImpl.class),
                        (DeserializationFunction<UnionConfig>) injector.getInstance(Key.get(TypeLiteral.get(
                                Types.newParameterizedType(DeserializationFunction.class, UnionConfig.class)))),
                        (SerializationFunction<UnionConfig>) injector.getInstance(Key.get(TypeLiteral.get(
                                Types.newParameterizedType(SerializationFunction.class, UnionConfig.class)))))
                .getOrThrow();

        UnionConfig unionConfig = classStringReaderProvider.get();

        assertThat(unionConfig).isNotNull();

        assertThat(unionConfig)
                .asInstanceOf(InstanceOfAssertFactories.type(UnionConfig.Child1Config.class))
                .extracting(UnionConfig.Child1Config::hello)
                .isEqualTo("hi");
    }

    @Test
    void testNoNoArgConstructorClassConfig() {
        var fileContents = "id: 42\nname: \"hello\"";

        var stringReaderProvider = injector.getInstance(ConfigProviderFactory.class)
                .createStringReaderProvider(
                        injector.getInstance(YamlFileType.class),
                        fileContents,
                        new Configuration<>(
                                null, NoNoArgConstructorConfigImpl.class, NoNoArgConstructorConfigImpl.class),
                        (DeserializationFunction<NoNoArgConstructorConfigImpl>)
                                injector.getInstance(Key.get(TypeLiteral.get(Types.newParameterizedType(
                                        DeserializationFunction.class, NoNoArgConstructorConfigImpl.class)))),
                        (SerializationFunction<NoNoArgConstructorConfigImpl>)
                                injector.getInstance(Key.get(TypeLiteral.get(Types.newParameterizedType(
                                        SerializationFunction.class, NoNoArgConstructorConfigImpl.class)))))
                .getOrThrow();

        NoNoArgConstructorConfigImpl config = stringReaderProvider.get();

        assertThat(config).isNotNull();
        assertThat(config.id()).isEqualTo(42);
        assertThat(config.name()).isEqualTo("hello");
    }

    @Test
    void testConstructorAndDefaultValueConfig() {
        var fileContents = "y: 42";

        var stringReaderProvider = injector.getInstance(ConfigProviderFactory.class)
                .createStringReaderProvider(
                        injector.getInstance(YamlFileType.class),
                        fileContents,
                        new Configuration<>(
                                null,
                                ConstructorAndDefaultValueConfigImpl.class,
                                ConstructorAndDefaultValueConfigImpl.class),
                        (DeserializationFunction<ConstructorAndDefaultValueConfigImpl>)
                                injector.getInstance(Key.get(TypeLiteral.get(Types.newParameterizedType(
                                        DeserializationFunction.class, ConstructorAndDefaultValueConfigImpl.class)))),
                        (SerializationFunction<ConstructorAndDefaultValueConfigImpl>)
                                injector.getInstance(Key.get(TypeLiteral.get(Types.newParameterizedType(
                                        SerializationFunction.class, ConstructorAndDefaultValueConfigImpl.class)))))
                .getOrThrow();

        ConstructorAndDefaultValueConfigImpl config = stringReaderProvider.get();

        assertThat(config).isNotNull();
        assertThat(config.x()).isEqualTo(3);
        assertThat(config.y()).isEqualTo(42);
    }

    @Test
    void testValidationConfigSuccess() {
        var fileContents = """
                positiveInt: 5
                negativeDouble: -2.5
                minInt: 15
                maxLong: 50
                rangeDouble: 3.5
                notBlankString: "hello"
                customValidated: "mitten-lib"
                """;

        var stringReaderProvider = injector.getInstance(ConfigProviderFactory.class)
                .createStringReaderProvider(
                        injector.getInstance(YamlFileType.class),
                        fileContents,
                        new Configuration<>(null, ValidationConfig.class),
                        (DeserializationFunction<ValidationConfig>) injector.getInstance(Key.get(TypeLiteral.get(
                                Types.newParameterizedType(DeserializationFunction.class, ValidationConfig.class)))),
                        (SerializationFunction<ValidationConfig>) injector.getInstance(Key.get(TypeLiteral.get(
                                Types.newParameterizedType(SerializationFunction.class, ValidationConfig.class)))))
                .getOrThrow();

        ValidationConfig config = stringReaderProvider.get();

        assertThat(config).isNotNull();
        assertThat(config.positiveInt()).isEqualTo(5);
        assertThat(config.negativeDouble()).isEqualTo(-2.5);
        assertThat(config.minInt()).isEqualTo(15);
        assertThat(config.maxLong()).isEqualTo(50L);
        assertThat(config.rangeDouble()).isEqualTo(3.5);
        assertThat(config.notBlankString()).isEqualTo("hello");
        assertThat(config.customValidated()).isEqualTo("mitten-lib");
    }

    @Test
    void testValidationConfigFailure() {
        var fileContents = """
                positiveInt: -5
                negativeDouble: 2.5
                minInt: 5
                maxLong: 150
                rangeDouble: 6.0
                notBlankString: "   "
                customValidated: "not-mitten"
                """;

        var provider = injector.getInstance(ConfigProviderFactory.class)
                .createStringReaderProvider(
                        injector.getInstance(YamlFileType.class),
                        fileContents,
                        new Configuration<>(null, ValidationConfig.class),
                        (DeserializationFunction<ValidationConfig>) injector.getInstance(Key.get(TypeLiteral.get(
                                Types.newParameterizedType(DeserializationFunction.class, ValidationConfig.class)))),
                        (SerializationFunction<ValidationConfig>) injector.getInstance(Key.get(TypeLiteral.get(
                                Types.newParameterizedType(SerializationFunction.class, ValidationConfig.class)))))
                .getOrThrow();

        assertThatThrownBy(provider::get)
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("Configuration validation failed for class ValidationConfig with 7 violation(s):")
                .hasMessageContaining("Property 'positiveInt' (invalid value: -5): Must be positive")
                .hasMessageContaining("Property 'negativeDouble' (invalid value: 2.5): Must be negative")
                .hasMessageContaining("Property 'minInt' (invalid value: 5): Must be at least 10.0")
                .hasMessageContaining("Property 'maxLong' (invalid value: 150): Must be at most 100.0")
                .hasMessageContaining("Property 'rangeDouble' (invalid value: 6.0): Must be between 1.0 and 5.0")
                .hasMessageContaining("Property 'notBlankString' (invalid value:    ): Must not be blank")
                .hasMessageContaining(
                        "Property 'customValidated' (invalid value: not-mitten): Must start with expected prefix, but was 'not-mitten'");
    }

    @Test
    void testValidationConfigInjection() {
        var localInjector = Guice.createInjector(
                new ConfigLoaderModule().asModuleWithInfrastructure(),
                new FileWatcherModule(),
                new FileTypeModule(),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(MittenLibConsumer.class).toInstance(new MittenLibConsumer("Tests"));
                        bind(CustomStringValidator.ValidationDependency.class)
                                .toInstance(new CustomStringValidator.ValidationDependency("guice-mitten"));
                    }
                });

        var fileContents = """
                positiveInt: 5
                negativeDouble: -2.5
                minInt: 15
                maxLong: 50
                rangeDouble: 3.5
                notBlankString: "hello"
                customValidated: "guice-mitten-lib"
                """;

        var provider = localInjector
                .getInstance(ConfigProviderFactory.class)
                .createStringReaderProvider(
                        localInjector.getInstance(YamlFileType.class),
                        fileContents,
                        new Configuration<>(null, ValidationConfig.class),
                        (DeserializationFunction<ValidationConfig>) localInjector.getInstance(Key.get(TypeLiteral.get(
                                Types.newParameterizedType(DeserializationFunction.class, ValidationConfig.class)))),
                        (SerializationFunction<ValidationConfig>) localInjector.getInstance(Key.get(TypeLiteral.get(
                                Types.newParameterizedType(SerializationFunction.class, ValidationConfig.class)))))
                .getOrThrow();

        ValidationConfig config = provider.get();
        assertThat(config).isNotNull();
        assertThat(config.customValidated()).isEqualTo("guice-mitten-lib");
    }

    @Test
    void testValidationConfigNullabilityFailure() {
        var fileContents = """
                positiveInt: 5
                negativeDouble: -2.5
                minInt: 15
                maxLong: 50
                rangeDouble: 3.5
                notBlankString: null
                customValidated: null
                """;

        var provider = injector.getInstance(ConfigProviderFactory.class)
                .createStringReaderProvider(
                        injector.getInstance(YamlFileType.class),
                        fileContents,
                        new Configuration<>(null, ValidationConfig.class, ValidationConfig.class),
                        (DeserializationFunction<ValidationConfig>) injector.getInstance(Key.get(TypeLiteral.get(
                                Types.newParameterizedType(DeserializationFunction.class, ValidationConfig.class)))),
                        (SerializationFunction<ValidationConfig>) injector.getInstance(Key.get(TypeLiteral.get(
                                Types.newParameterizedType(SerializationFunction.class, ValidationConfig.class)))))
                .getOrThrow();

        assertThatThrownBy(provider::get)
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("Configuration validation failed for class ValidationConfig with 2 violation(s):")
                .hasMessageContaining("Property 'notBlankString' (invalid value: null): Must not be null")
                .hasMessageContaining("Property 'customValidated' (invalid value: null): Must not be null");
    }

    @Test
    void testValidationConfigProgrammatic() {
        ValidationConfig config = new ValidationConfig(
                -5, // positiveInt (invalid)
                2.5, // negativeDouble (invalid)
                5, // minInt (invalid)
                150, // maxLong (invalid)
                6.0, // rangeDouble (invalid)
                "   ", // notBlankString (invalid)
                "not-mitten" // customValidated (invalid)
                );

        Result<ValidationConfig> result =
                injector.getInstance(ValidationConfigValidator.class).validate(config);
        assertThat(result.isFailure()).isTrue();
        Exception exception = result.error().orElseThrow();
        assertThat(exception).isInstanceOf(ConfigValidationException.class);

        String message = exception.getMessage();
        assertThat(message).contains("Configuration validation failed for class ValidationConfig with 7 violation(s):");
        assertThat(message).contains("Property 'positiveInt' (invalid value: -5): Must be positive");
        assertThat(message).contains("Property 'negativeDouble' (invalid value: 2.5): Must be negative");
        assertThat(message).contains("Property 'minInt' (invalid value: 5): Must be at least 10.0");
        assertThat(message).contains("Property 'maxLong' (invalid value: 150): Must be at most 100.0");
        assertThat(message).contains("Property 'rangeDouble' (invalid value: 6.0): Must be between 1.0 and 5.0");
        assertThat(message).contains("Property 'notBlankString' (invalid value:    ): Must not be blank");
        assertThat(message)
                .contains(
                        "Property 'customValidated' (invalid value: not-mitten): Must start with expected prefix, but was 'not-mitten'");
    }

    @Test
    void testCollectionValidationConfigSuccess() {
        var fileContents = """
                names:
                  - "alex"
                  - "bob"
                values:
                  - 1
                  - 2
                scores:
                  alex: 10
                  bob: 20
                nullableNames:
                  - "charlie"
                  - null
                customList:
                  - "mitten-lib-1"
                  - "mitten-lib-2"
                customMap:
                  alex: "mitten-lib-3"
                """;

        var provider = injector.getInstance(ConfigProviderFactory.class)
                .createStringReaderProvider(
                        injector.getInstance(YamlFileType.class),
                        fileContents,
                        new Configuration<>(null, CollectionValidationConfig.class),
                        (DeserializationFunction<CollectionValidationConfig>)
                                injector.getInstance(Key.get(TypeLiteral.get(Types.newParameterizedType(
                                        DeserializationFunction.class, CollectionValidationConfig.class)))),
                        (SerializationFunction<CollectionValidationConfig>)
                                injector.getInstance(Key.get(TypeLiteral.get(Types.newParameterizedType(
                                        SerializationFunction.class, CollectionValidationConfig.class)))))
                .getOrThrow();

        CollectionValidationConfig config = provider.get();

        assertThat(config).isNotNull();
        assertThat(config.names()).containsExactly("alex", "bob");
        assertThat(config.values()).containsExactlyInAnyOrder(1, 2);
        assertThat(config.scores()).containsEntry("alex", 10).containsEntry("bob", 20);
        assertThat(config.nullableNames()).containsExactly("charlie", null);
        assertThat(config.customList()).containsExactly("mitten-lib-1", "mitten-lib-2");
        assertThat(config.customMap()).containsEntry("alex", "mitten-lib-3");
    }

    @Test
    void testCollectionValidationConfigFailure() {
        var fileContents = """
                names:
                  - "   "
                  - "bob"
                values:
                  - -1
                  - 2
                scores:
                  "": 10
                  bob: -5
                nullableNames:
                  - "   "
                customList:
                  - "invalid-1"
                customMap:
                  alex: "invalid-2"
                """;

        var provider = injector.getInstance(ConfigProviderFactory.class)
                .createStringReaderProvider(
                        injector.getInstance(YamlFileType.class),
                        fileContents,
                        new Configuration<>(null, CollectionValidationConfig.class),
                        (DeserializationFunction<CollectionValidationConfig>)
                                injector.getInstance(Key.get(TypeLiteral.get(Types.newParameterizedType(
                                        DeserializationFunction.class, CollectionValidationConfig.class)))),
                        (SerializationFunction<CollectionValidationConfig>)
                                injector.getInstance(Key.get(TypeLiteral.get(Types.newParameterizedType(
                                        SerializationFunction.class, CollectionValidationConfig.class)))))
                .getOrThrow();

        assertThatThrownBy(provider::get)
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("Configuration validation failed for class CollectionValidationConfig")
                .hasMessageContaining("Property 'names[0]' (invalid value:    ): Must not be blank")
                .hasMessageContaining("Property 'values[-1]' (invalid value: -1): Must be positive")
                .hasMessageContaining("Property 'scores[]' (invalid value: ): Key Must not be blank")
                .hasMessageContaining("Property 'scores[bob]' (invalid value: -5): Must be at least 0.0")
                .hasMessageContaining("Property 'nullableNames[0]' (invalid value:    ): Must not be blank")
                .hasMessageContaining(
                        "Property 'customList[0]' (invalid value: invalid-1): Must start with expected prefix, but was 'invalid-1'")
                .hasMessageContaining(
                        "Property 'customMap[alex]' (invalid value: invalid-2): Must start with expected prefix, but was 'invalid-2'");
    }

    @Test
    void testCollectionValidationConfigNullability() {
        var fileContents = """
                names:
                  - null
                values:
                  - 1
                scores:
                  alex: null
                nullableNames:
                  - null
                customList:
                  - null
                customMap:
                  alex: null
                """;

        var provider = injector.getInstance(ConfigProviderFactory.class)
                .createStringReaderProvider(
                        injector.getInstance(YamlFileType.class),
                        fileContents,
                        new Configuration<>(null, CollectionValidationConfig.class),
                        (DeserializationFunction<CollectionValidationConfig>)
                                injector.getInstance(Key.get(TypeLiteral.get(Types.newParameterizedType(
                                        DeserializationFunction.class, CollectionValidationConfig.class)))),
                        (SerializationFunction<CollectionValidationConfig>)
                                injector.getInstance(Key.get(TypeLiteral.get(Types.newParameterizedType(
                                        SerializationFunction.class, CollectionValidationConfig.class)))))
                .getOrThrow();

        assertThatThrownBy(provider::get)
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("Configuration validation failed for class CollectionValidationConfig")
                .hasMessageContaining("Property 'names[0]' (invalid value: null): Must not be null")
                .hasMessageContaining("Property 'scores[alex]' (invalid value: null): Must not be null")
                .hasMessageContaining("Property 'customList[0]' (invalid value: null): Must not be null")
                .hasMessageContaining("Property 'customMap[alex]' (invalid value: null): Must not be null");
    }

    @Test
    void testCollectionValidationConfigProgrammaticNullKey() {
        var scores = new HashMap<String, Integer>();
        scores.put(null, 10);

        var config = new CollectionValidationConfig(
                List.of("alex"), Set.of(1), scores, List.of(), List.of("mitten-lib-1"), Map.of());

        var result =
                injector.getInstance(CollectionValidationConfigValidator.class).validate(config);
        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().orElseThrow())
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("Property 'scores[null]' (invalid value: null): Key must not be null");
    }
}
