package me.bristermitten.mittenlib.annotations.integration

import com.google.inject.{AbstractModule, Guice, Injector, Key, TypeLiteral}
import com.google.inject.util.Types
import java.io.IOException
import java.util.HashMap
import java.util.List
import java.util.Map
import java.util.Set
import me.bristermitten.mittenlib.MittenLibConsumer
import me.bristermitten.mittenlib.annotations.util.IntegrationTests.loadResourceString
import me.bristermitten.mittenlib.config.{
  Configuration,
  DeserializationFunction,
  SerializationContext,
  SerializationFunction
}
import me.bristermitten.mittenlib.config.exception.ConfigValidationException
import me.bristermitten.mittenlib.config.provider.construct.ConfigProviderFactory
import me.bristermitten.mittenlib.config.reader.ObjectMapper
import me.bristermitten.mittenlib.config.tree.DataTree
import me.bristermitten.mittenlib.files.FileTypeModule
import me.bristermitten.mittenlib.files.yaml.YamlFileType
import me.bristermitten.mittenlib.watcher.FileWatcherModule
import org.assertj.core.api.Assertions.assertThat
import org.scalatest.BeforeAndAfterEach
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class IntegrationTest
    extends AnyFunSuite
    with Matchers
    with BeforeAndAfterEach {

  private var injector: Injector = _

  override def beforeEach(): Unit = {
    injector = Guice.createInjector(
      new ConfigLoaderModule().asModuleWithInfrastructure(),
      new FileWatcherModule(),
      new FileTypeModule(),
      new AbstractModule() {
        override def configure(): Unit = {
          bind(classOf[MittenLibConsumer]).toInstance(
            new MittenLibConsumer("Tests")
          )
        }
      }
    )
  }

  test("testBindingExists") {
    val key = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[DeserializationFunction[_]],
            classOf[InterfaceConfig]
          )
        )
      )
      .asInstanceOf[Key[DeserializationFunction[InterfaceConfig]]]
    val instance = injector.getInstance(key)
    instance should not be null
  }

  test("testInterfaceConfig") {
    val fileContents =
      loadResourceString("integration/InterfaceConfig_dummy.yml")

    val deserializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[DeserializationFunction[_]],
            classOf[InterfaceConfig]
          )
        )
      )
      .asInstanceOf[Key[DeserializationFunction[InterfaceConfig]]]

    val serializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[SerializationFunction[_]],
            classOf[InterfaceConfig]
          )
        )
      )
      .asInstanceOf[Key[SerializationFunction[InterfaceConfig]]]

    val stringReaderProvider = injector
      .getInstance(classOf[ConfigProviderFactory])
      .createStringReaderProvider(
        injector.getInstance(classOf[YamlFileType]),
        fileContents,
        new Configuration(
          null,
          classOf[InterfaceConfig],
          classOf[InterfaceConfigImpl]
        ),
        injector.getInstance(deserializerKey),
        injector.getInstance(serializerKey)
      )
      .getOrThrow()

    val interfaceConfig = stringReaderProvider.get()

    interfaceConfig should not be null
    interfaceConfig.name() shouldBe "a"

    import scala.jdk.CollectionConverters.*
    val children = interfaceConfig.children().asScala.toList
    children should have size 1
    children.head shouldBe new InterfaceConfigImpl(
      "b",
      4,
      List.of(new InterfaceConfigImpl("c", 5, List.of(), null)),
      null
    )

    interfaceConfig.child() should not be null
    interfaceConfig.child().id() shouldBe "pee"
  }

  test("testClassConfig") {
    val fileContents =
      loadResourceString("integration/InterfaceConfig_dummy.yml")

    val deserializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[DeserializationFunction[_]],
            classOf[ClassConfigImpl]
          )
        )
      )
      .asInstanceOf[Key[DeserializationFunction[ClassConfigImpl]]]

    val serializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[SerializationFunction[_]],
            classOf[ClassConfigImpl]
          )
        )
      )
      .asInstanceOf[Key[SerializationFunction[ClassConfigImpl]]]

    val stringReaderProvider = injector
      .getInstance(classOf[ConfigProviderFactory])
      .createStringReaderProvider(
        injector.getInstance(classOf[YamlFileType]),
        fileContents,
        new Configuration(
          null,
          classOf[ClassConfigImpl],
          classOf[ClassConfigImpl]
        ),
        injector.getInstance(deserializerKey),
        injector.getInstance(serializerKey)
      )
      .getOrThrow()

    val classConfig = stringReaderProvider.get()

    classConfig should not be null
    classConfig.name() shouldBe "a"

    import scala.jdk.CollectionConverters.*
    val children = classConfig.children().asScala.toList
    children should have size 1
    children.head shouldBe new InterfaceConfigImpl(
      "b",
      4,
      List.of(new InterfaceConfigImpl("c", 5, List.of(), null)),
      null
    )

    classConfig.child() should not be null
    classConfig.child().id() shouldBe "pee"
    classConfig.defaultValue() shouldBe 1
  }

  test("testClassConfigIdenticalToInterface") {
    val fileContents =
      loadResourceString("integration/InterfaceConfig_dummy.yml")

    val classDeserializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[DeserializationFunction[_]],
            classOf[ClassConfigImpl]
          )
        )
      )
      .asInstanceOf[Key[DeserializationFunction[ClassConfigImpl]]]

    val classSerializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[SerializationFunction[_]],
            classOf[ClassConfigImpl]
          )
        )
      )
      .asInstanceOf[Key[SerializationFunction[ClassConfigImpl]]]

    val interfaceDeserializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[DeserializationFunction[_]],
            classOf[InterfaceConfig]
          )
        )
      )
      .asInstanceOf[Key[DeserializationFunction[InterfaceConfig]]]

    val interfaceSerializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[SerializationFunction[_]],
            classOf[InterfaceConfig]
          )
        )
      )
      .asInstanceOf[Key[SerializationFunction[InterfaceConfig]]]

    val classStringReaderProvider = injector
      .getInstance(classOf[ConfigProviderFactory])
      .createStringReaderProvider(
        injector.getInstance(classOf[YamlFileType]),
        fileContents,
        new Configuration(
          null,
          classOf[ClassConfigImpl],
          classOf[ClassConfigImpl]
        ),
        injector.getInstance(classDeserializerKey),
        injector.getInstance(classSerializerKey)
      )
      .getOrThrow()

    val interfaceStringReaderProvider = injector
      .getInstance(classOf[ConfigProviderFactory])
      .createStringReaderProvider(
        injector.getInstance(classOf[YamlFileType]),
        fileContents,
        new Configuration(
          null,
          classOf[InterfaceConfig],
          classOf[InterfaceConfigImpl]
        ),
        injector.getInstance(interfaceDeserializerKey),
        injector.getInstance(interfaceSerializerKey)
      )
      .getOrThrow()

    val interfaceConfig = interfaceStringReaderProvider.get()
    val config = classStringReaderProvider.get()

    interfaceConfig.name() shouldBe config.name()
    interfaceConfig.age() shouldBe config.age()

    assertThat(interfaceConfig.children())
      .usingRecursiveComparison()
      .withEqualsForFields(
        (a: InterfaceConfig.ChildConfig, b: ClassConfigImpl.ChildConfigImpl) =>
          a.id() == b.id()
      )
      .isEqualTo(config.children())

    assertThat(interfaceConfig.child())
      .isNotNull()
      .usingRecursiveComparison()
      .isEqualTo(config.child())
  }

  test("testIntersectionConfigParsing") {
    val fileContents =
      loadResourceString("integration/IntersectionConfig_1.yml")

    val deserializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[DeserializationFunction[_]],
            classOf[IntersectionConfig]
          )
        )
      )
      .asInstanceOf[Key[DeserializationFunction[IntersectionConfig]]]

    val serializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[SerializationFunction[_]],
            classOf[IntersectionConfig]
          )
        )
      )
      .asInstanceOf[Key[SerializationFunction[IntersectionConfig]]]

    val intersectionConfig = injector
      .getInstance(classOf[ConfigProviderFactory])
      .createStringReaderProvider(
        injector.getInstance(classOf[YamlFileType]),
        fileContents,
        new Configuration(
          null,
          classOf[IntersectionConfig],
          classOf[IntersectionConfigImpl]
        ),
        injector.getInstance(deserializerKey),
        injector.getInstance(serializerKey)
      )
      .getOrThrow()
      .get()

    intersectionConfig should not be null
    intersectionConfig.base() shouldBe "hello"
  }

  test("testIntersectionConfigParsingChild") {
    val fileContents =
      loadResourceString("integration/IntersectionConfig_2.yml")

    val deserializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[DeserializationFunction[_]],
            classOf[IntersectionConfig.ChildIntersectionConfig]
          )
        )
      )
      .asInstanceOf[Key[
        DeserializationFunction[IntersectionConfig.ChildIntersectionConfig]
      ]]

    val serializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[SerializationFunction[_]],
            classOf[IntersectionConfig.ChildIntersectionConfig]
          )
        )
      )
      .asInstanceOf[Key[
        SerializationFunction[IntersectionConfig.ChildIntersectionConfig]
      ]]

    val intersectionConfig2 = injector
      .getInstance(classOf[ConfigProviderFactory])
      .createStringReaderProvider(
        injector.getInstance(classOf[YamlFileType]),
        fileContents,
        new Configuration(
          null,
          classOf[IntersectionConfig.ChildIntersectionConfig],
          classOf[IntersectionConfigImpl.ChildIntersectionConfigImpl]
        ),
        injector.getInstance(deserializerKey),
        injector.getInstance(serializerKey)
      )
      .getOrThrow()
      .get()

    intersectionConfig2 should not be null
    intersectionConfig2 shouldBe a[IntersectionConfig.ChildIntersectionConfig]
    intersectionConfig2.extra() shouldBe "wow"
    intersectionConfig2.base() shouldBe "hello"
  }

  test("testUnionConfigParsing") {
    val fileContents = loadResourceString("integration/UnionConfig_dummy.yml")

    val deserializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[DeserializationFunction[_]],
            classOf[UnionConfig]
          )
        )
      )
      .asInstanceOf[Key[DeserializationFunction[UnionConfig]]]

    val serializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[SerializationFunction[_]],
            classOf[UnionConfig]
          )
        )
      )
      .asInstanceOf[Key[SerializationFunction[UnionConfig]]]

    val classStringReaderProvider = injector
      .getInstance(classOf[ConfigProviderFactory])
      .createStringReaderProvider(
        injector.getInstance(classOf[YamlFileType]),
        fileContents,
        new Configuration(null, classOf[UnionConfig], classOf[UnionConfigImpl]),
        injector.getInstance(deserializerKey),
        injector.getInstance(serializerKey)
      )
      .getOrThrow()

    val unionConfig = classStringReaderProvider.get()

    unionConfig should not be null
    unionConfig shouldBe a[UnionConfig.Child1Config]
    unionConfig.asInstanceOf[UnionConfig.Child1Config].hello() shouldBe "hi"
  }

  test("testNoNoArgConstructorClassConfig") {
    val fileContents = "id: 42\nname: \"hello\""

    val deserializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[DeserializationFunction[_]],
            classOf[NoNoArgConstructorConfigImpl]
          )
        )
      )
      .asInstanceOf[Key[DeserializationFunction[NoNoArgConstructorConfigImpl]]]

    val serializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[SerializationFunction[_]],
            classOf[NoNoArgConstructorConfigImpl]
          )
        )
      )
      .asInstanceOf[Key[SerializationFunction[NoNoArgConstructorConfigImpl]]]

    val stringReaderProvider = injector
      .getInstance(classOf[ConfigProviderFactory])
      .createStringReaderProvider(
        injector.getInstance(classOf[YamlFileType]),
        fileContents,
        new Configuration(
          null,
          classOf[NoNoArgConstructorConfigImpl],
          classOf[NoNoArgConstructorConfigImpl]
        ),
        injector.getInstance(deserializerKey),
        injector.getInstance(serializerKey)
      )
      .getOrThrow()

    val config = stringReaderProvider.get()

    config should not be null
    config.id() shouldBe 42
    config.name() shouldBe "hello"
  }

  test("testConstructorAndDefaultValueConfig") {
    val fileContents = "y: 42"

    val deserializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[DeserializationFunction[_]],
            classOf[ConstructorAndDefaultValueConfigImpl]
          )
        )
      )
      .asInstanceOf[Key[
        DeserializationFunction[ConstructorAndDefaultValueConfigImpl]
      ]]

    val serializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[SerializationFunction[_]],
            classOf[ConstructorAndDefaultValueConfigImpl]
          )
        )
      )
      .asInstanceOf[Key[
        SerializationFunction[ConstructorAndDefaultValueConfigImpl]
      ]]

    val stringReaderProvider = injector
      .getInstance(classOf[ConfigProviderFactory])
      .createStringReaderProvider(
        injector.getInstance(classOf[YamlFileType]),
        fileContents,
        new Configuration(
          null,
          classOf[ConstructorAndDefaultValueConfigImpl],
          classOf[ConstructorAndDefaultValueConfigImpl]
        ),
        injector.getInstance(deserializerKey),
        injector.getInstance(serializerKey)
      )
      .getOrThrow()

    val config = stringReaderProvider.get()

    config should not be null
    config.x() shouldBe 3
    config.y() shouldBe 42
  }

  test("testValidationConfigSuccess") {
    val fileContents =
      """
        |positiveInt: 5
        |negativeDouble: -2.5
        |minInt: 15
        |maxLong: 50
        |rangeDouble: 3.5
        |notBlankString: "hello"
        |customValidated: "mitten-lib"
        |""".stripMargin

    val deserializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[DeserializationFunction[_]],
            classOf[ValidationConfig]
          )
        )
      )
      .asInstanceOf[Key[DeserializationFunction[ValidationConfig]]]

    val serializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[SerializationFunction[_]],
            classOf[ValidationConfig]
          )
        )
      )
      .asInstanceOf[Key[SerializationFunction[ValidationConfig]]]

    val stringReaderProvider = injector
      .getInstance(classOf[ConfigProviderFactory])
      .createStringReaderProvider(
        injector.getInstance(classOf[YamlFileType]),
        fileContents,
        new Configuration(null, classOf[ValidationConfig]),
        injector.getInstance(deserializerKey),
        injector.getInstance(serializerKey)
      )
      .getOrThrow()

    val config = stringReaderProvider.get()

    config should not be null
    config.positiveInt() shouldBe 5
    config.negativeDouble() shouldBe -2.5
    config.minInt() shouldBe 15
    config.maxLong() shouldBe 50L
    config.rangeDouble() shouldBe 3.5
    config.notBlankString() shouldBe "hello"
    config.customValidated() shouldBe "mitten-lib"
  }

  test("testValidationConfigFailure") {
    val fileContents =
      """
        |positiveInt: -5
        |negativeDouble: 2.5
        |minInt: 5
        |maxLong: 150
        |rangeDouble: 6.0
        |notBlankString: "   "
        |customValidated: "not-mitten"
        |""".stripMargin

    val deserializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[DeserializationFunction[_]],
            classOf[ValidationConfig]
          )
        )
      )
      .asInstanceOf[Key[DeserializationFunction[ValidationConfig]]]

    val serializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[SerializationFunction[_]],
            classOf[ValidationConfig]
          )
        )
      )
      .asInstanceOf[Key[SerializationFunction[ValidationConfig]]]

    val provider = injector
      .getInstance(classOf[ConfigProviderFactory])
      .createStringReaderProvider(
        injector.getInstance(classOf[YamlFileType]),
        fileContents,
        new Configuration(null, classOf[ValidationConfig]),
        injector.getInstance(deserializerKey),
        injector.getInstance(serializerKey)
      )
      .getOrThrow()

    val exception = intercept[ConfigValidationException] {
      provider.get()
    }
    val message = exception.getMessage
    message should include(
      "Configuration validation failed for class ValidationConfig with 7 violation(s):"
    )
    message should include(
      "Property 'positiveInt' (invalid value: -5): Must be positive"
    )
    message should include(
      "Property 'negativeDouble' (invalid value: 2.5): Must be negative"
    )
    message should include(
      "Property 'minInt' (invalid value: 5): Must be at least 10.0"
    )
    message should include(
      "Property 'maxLong' (invalid value: 150): Must be at most 100.0"
    )
    message should include(
      "Property 'rangeDouble' (invalid value: 6.0): Must be between 1.0 and 5.0"
    )
    message should include(
      "Property 'notBlankString' (invalid value:    ): Must not be blank"
    )
    message should include(
      "Property 'customValidated' (invalid value: not-mitten): Must start with expected prefix, but was 'not-mitten'"
    )
  }

  test("testValidationConfigInjection") {
    val localInjector = Guice.createInjector(
      new ConfigLoaderModule().asModuleWithInfrastructure(),
      new FileWatcherModule(),
      new FileTypeModule(),
      new AbstractModule() {
        override def configure(): Unit = {
          bind(classOf[MittenLibConsumer])
            .toInstance(new MittenLibConsumer("Tests"))
          bind(classOf[CustomStringValidator.ValidationDependency])
            .toInstance(
              new CustomStringValidator.ValidationDependency("guice-mitten")
            )
        }
      }
    )

    val fileContents =
      """
        |positiveInt: 5
        |negativeDouble: -2.5
        |minInt: 15
        |maxLong: 50
        |rangeDouble: 3.5
        |notBlankString: "hello"
        |customValidated: "guice-mitten-lib"
        |""".stripMargin

    val deserializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[DeserializationFunction[_]],
            classOf[ValidationConfig]
          )
        )
      )
      .asInstanceOf[Key[DeserializationFunction[ValidationConfig]]]

    val serializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[SerializationFunction[_]],
            classOf[ValidationConfig]
          )
        )
      )
      .asInstanceOf[Key[SerializationFunction[ValidationConfig]]]

    val provider = localInjector
      .getInstance(classOf[ConfigProviderFactory])
      .createStringReaderProvider(
        localInjector.getInstance(classOf[YamlFileType]),
        fileContents,
        new Configuration(null, classOf[ValidationConfig]),
        localInjector.getInstance(deserializerKey),
        localInjector.getInstance(serializerKey)
      )
      .getOrThrow()

    val config = provider.get()
    config should not be null
    config.customValidated() shouldBe "guice-mitten-lib"
  }

  test("testValidationConfigNullabilityFailure") {
    val fileContents =
      """
        |positiveInt: 5
        |negativeDouble: -2.5
        |minInt: 15
        |maxLong: 50
        |rangeDouble: 3.5
        |notBlankString: null
        |customValidated: null
        |""".stripMargin

    val deserializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[DeserializationFunction[_]],
            classOf[ValidationConfig]
          )
        )
      )
      .asInstanceOf[Key[DeserializationFunction[ValidationConfig]]]

    val serializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[SerializationFunction[_]],
            classOf[ValidationConfig]
          )
        )
      )
      .asInstanceOf[Key[SerializationFunction[ValidationConfig]]]

    val provider = injector
      .getInstance(classOf[ConfigProviderFactory])
      .createStringReaderProvider(
        injector.getInstance(classOf[YamlFileType]),
        fileContents,
        new Configuration(
          null,
          classOf[ValidationConfig],
          classOf[ValidationConfig]
        ),
        injector.getInstance(deserializerKey),
        injector.getInstance(serializerKey)
      )
      .getOrThrow()

    val exception = intercept[ConfigValidationException] {
      provider.get()
    }
    val message = exception.getMessage
    message should include(
      "Configuration validation failed for class ValidationConfig with 2 violation(s):"
    )
    message should include(
      "Property 'notBlankString' (invalid value: null): Must not be null"
    )
    message should include(
      "Property 'customValidated' (invalid value: null): Must not be null"
    )
  }

  test("testValidationConfigProgrammatic") {
    val config = new ValidationConfig(
      -5, // positiveInt (invalid)
      2.5, // negativeDouble (invalid)
      5, // minInt (invalid)
      150, // maxLong (invalid)
      6.0, // rangeDouble (invalid)
      "   ", // notBlankString (invalid)
      "not-mitten" // customValidated (invalid)
    )

    val result =
      injector.getInstance(classOf[ValidationConfigValidator]).validate(config)
    result.isFailure shouldBe true
    val exception = result.error().orElseThrow()
    exception shouldBe a[ConfigValidationException]

    val message = exception.getMessage
    message should include(
      "Configuration validation failed for class ValidationConfig with 7 violation(s):"
    )
    message should include(
      "Property 'positiveInt' (invalid value: -5): Must be positive"
    )
    message should include(
      "Property 'negativeDouble' (invalid value: 2.5): Must be negative"
    )
    message should include(
      "Property 'minInt' (invalid value: 5): Must be at least 10.0"
    )
    message should include(
      "Property 'maxLong' (invalid value: 150): Must be at most 100.0"
    )
    message should include(
      "Property 'rangeDouble' (invalid value: 6.0): Must be between 1.0 and 5.0"
    )
    message should include(
      "Property 'notBlankString' (invalid value:    ): Must not be blank"
    )
    message should include(
      "Property 'customValidated' (invalid value: not-mitten): Must start with expected prefix, but was 'not-mitten'"
    )
  }

  test("testCollectionValidationConfigSuccess") {
    val fileContents =
      """
        |names:
        |  - "alex"
        |  - "bob"
        |values:
        |  - 1
        |  - 2
        |scores:
        |  alex: 10
        |  bob: 20
        |nullableNames:
        |  - "charlie"
        |  - null
        |customList:
        |  - "mitten-lib-1"
        |  - "mitten-lib-2"
        |customMap:
        |  alex: "mitten-lib-3"
        |""".stripMargin

    val deserializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[DeserializationFunction[_]],
            classOf[CollectionValidationConfig]
          )
        )
      )
      .asInstanceOf[Key[DeserializationFunction[CollectionValidationConfig]]]

    val serializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[SerializationFunction[_]],
            classOf[CollectionValidationConfig]
          )
        )
      )
      .asInstanceOf[Key[SerializationFunction[CollectionValidationConfig]]]

    val provider = injector
      .getInstance(classOf[ConfigProviderFactory])
      .createStringReaderProvider(
        injector.getInstance(classOf[YamlFileType]),
        fileContents,
        new Configuration(null, classOf[CollectionValidationConfig]),
        injector.getInstance(deserializerKey),
        injector.getInstance(serializerKey)
      )
      .getOrThrow()

    val config = provider.get()

    config should not be null
    assertThat(config.names()).containsExactly("alex", "bob")
    assertThat(config.values()).containsExactlyInAnyOrder(1, 2)
    assertThat(config.scores())
      .containsEntry("alex", 10)
      .containsEntry("bob", 20)
    assertThat(config.nullableNames()).containsExactly("charlie", null)
    assertThat(config.customList())
      .containsExactly("mitten-lib-1", "mitten-lib-2")
    assertThat(config.customMap()).containsEntry("alex", "mitten-lib-3")
  }

  test("testCollectionValidationConfigFailure") {
    val fileContents =
      """
        |names:
        |  - "   "
        |  - "bob"
        |values:
        |  - -1
        |  - 2
        |scores:
        |  "": 10
        |  bob: -5
        |nullableNames:
        |  - "   "
        |customList:
        |  - "invalid-1"
        |customMap:
        |  alex: "invalid-2"
        |""".stripMargin

    val deserializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[DeserializationFunction[_]],
            classOf[CollectionValidationConfig]
          )
        )
      )
      .asInstanceOf[Key[DeserializationFunction[CollectionValidationConfig]]]

    val serializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[SerializationFunction[_]],
            classOf[CollectionValidationConfig]
          )
        )
      )
      .asInstanceOf[Key[SerializationFunction[CollectionValidationConfig]]]

    val provider = injector
      .getInstance(classOf[ConfigProviderFactory])
      .createStringReaderProvider(
        injector.getInstance(classOf[YamlFileType]),
        fileContents,
        new Configuration(null, classOf[CollectionValidationConfig]),
        injector.getInstance(deserializerKey),
        injector.getInstance(serializerKey)
      )
      .getOrThrow()

    val exception = intercept[ConfigValidationException] {
      provider.get()
    }
    val message = exception.getMessage
    message should include(
      "Configuration validation failed for class CollectionValidationConfig"
    )
    message should include(
      "Property 'names[0]' (invalid value:    ): Must not be blank"
    )
    message should include(
      "Property 'values[-1]' (invalid value: -1): Must be positive"
    )
    message should include(
      "Property 'scores[]' (invalid value: ): Key Must not be blank"
    )
    message should include(
      "Property 'scores[bob]' (invalid value: -5): Must be at least 0.0"
    )
    message should include(
      "Property 'nullableNames[0]' (invalid value:    ): Must not be blank"
    )
    message should include(
      "Property 'customList[0]' (invalid value: invalid-1): Must start with expected prefix, but was 'invalid-1'"
    )
    message should include(
      "Property 'customMap[alex]' (invalid value: invalid-2): Must start with expected prefix, but was 'invalid-2'"
    )
  }

  test("testCollectionValidationConfigNullability") {
    val fileContents =
      """
        |names:
        |  - null
        |values:
        |  - 1
        |scores:
        |  alex: null
        |nullableNames:
        |  - null
        |customList:
        |  - null
        |customMap:
        |  alex: null
        |""".stripMargin

    val deserializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[DeserializationFunction[_]],
            classOf[CollectionValidationConfig]
          )
        )
      )
      .asInstanceOf[Key[DeserializationFunction[CollectionValidationConfig]]]

    val serializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[SerializationFunction[_]],
            classOf[CollectionValidationConfig]
          )
        )
      )
      .asInstanceOf[Key[SerializationFunction[CollectionValidationConfig]]]

    val provider = injector
      .getInstance(classOf[ConfigProviderFactory])
      .createStringReaderProvider(
        injector.getInstance(classOf[YamlFileType]),
        fileContents,
        new Configuration(null, classOf[CollectionValidationConfig]),
        injector.getInstance(deserializerKey),
        injector.getInstance(serializerKey)
      )
      .getOrThrow()

    val exception = intercept[ConfigValidationException] {
      provider.get()
    }
    val message = exception.getMessage
    message should include(
      "Configuration validation failed for class CollectionValidationConfig"
    )
    message should include(
      "Property 'names[0]' (invalid value: null): Must not be null"
    )
    message should include(
      "Property 'scores[alex]' (invalid value: null): Must not be null"
    )
    message should include(
      "Property 'customList[0]' (invalid value: null): Must not be null"
    )
    message should include(
      "Property 'customMap[alex]' (invalid value: null): Must not be null"
    )
  }

  test("testCollectionValidationConfigProgrammaticNullKey") {
    val scores = new HashMap[String, Integer]()
    scores.put(null, 10)

    val config = new CollectionValidationConfig(
      List.of("alex"),
      Set.of(1),
      scores,
      List.of(),
      List.of("mitten-lib-1"),
      Map.of()
    )

    val result = injector
      .getInstance(classOf[CollectionValidationConfigValidator])
      .validate(config)
    result.isFailure shouldBe true
    val exception = result.error().orElseThrow()
    exception shouldBe a[ConfigValidationException]
    exception.getMessage should include(
      "Property 'scores[null]' (invalid value: null): Key must not be null"
    )
  }

  test("testNewtypeConfig") {
    val fileContents = "recordKey: \"hello\"\ninterfaceKey: 42\n"

    val deserializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[DeserializationFunction[_]],
            classOf[NewtypeConfig]
          )
        )
      )
      .asInstanceOf[Key[DeserializationFunction[NewtypeConfig]]]

    val serializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[SerializationFunction[_]],
            classOf[NewtypeConfig]
          )
        )
      )
      .asInstanceOf[Key[SerializationFunction[NewtypeConfig]]]

    val stringReaderProvider = injector
      .getInstance(classOf[ConfigProviderFactory])
      .createStringReaderProvider(
        injector.getInstance(classOf[YamlFileType]),
        fileContents,
        new Configuration(null, classOf[NewtypeConfig], classOf[NewtypeConfig]),
        injector.getInstance(deserializerKey),
        injector.getInstance(serializerKey)
      )
      .getOrThrow()

    val config = stringReaderProvider.get()

    config should not be null
    config.recordKey() should not be null
    config.recordKey().id() shouldBe "hello"
    config.interfaceKey() should not be null
    config.interfaceKey().value() shouldBe 42

    // Test Serialization
    val serializer = injector.getInstance(serializerKey)
    val dataTree = serializer.apply(
      config,
      new me.bristermitten.mittenlib.config.SerializationContext(
        injector.getInstance(
          classOf[me.bristermitten.mittenlib.config.reader.ObjectMapper]
        )
      )
    )
    dataTree should not be null
    dataTree shouldBe a[
      me.bristermitten.mittenlib.config.tree.DataTree.DataTreeMap
    ]

    val map = dataTree
      .asInstanceOf[me.bristermitten.mittenlib.config.tree.DataTree.DataTreeMap]
    val recordKeyTree = map.get("recordKey")
    recordKeyTree shouldBe a[
      me.bristermitten.mittenlib.config.tree.DataTree.DataTreeLiteral.DataTreeLiteralString
    ]
    recordKeyTree.value() shouldBe "hello"

    val interfaceKeyTree = map.get("interfaceKey")
    interfaceKeyTree shouldBe a[
      me.bristermitten.mittenlib.config.tree.DataTree.DataTreeLiteral.DataTreeLiteralInt
    ]
    interfaceKeyTree.value() shouldBe java.lang.Long.valueOf(42L)
  }
}
