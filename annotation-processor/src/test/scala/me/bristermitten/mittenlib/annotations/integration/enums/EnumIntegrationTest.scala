package me.bristermitten.mittenlib.annotations.integration.enums

import com.google.inject.{AbstractModule, Guice, Injector, Key, TypeLiteral}
import com.google.inject.util.Types
import me.bristermitten.mittenlib.MittenLibConsumer
import me.bristermitten.mittenlib.annotations.integration.ConfigLoaderModule
import me.bristermitten.mittenlib.annotations.util.IntegrationTests.loadResourceString
import me.bristermitten.mittenlib.config.{
  Configuration,
  DeserializationFunction,
  SerializationContext,
  SerializationFunction
}
import me.bristermitten.mittenlib.config.exception.InvalidEnumValueException
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

class EnumIntegrationTest
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
            new MittenLibConsumer("EnumTests")
          )
        }
      }
    )
  }

  test("testEnums") {
    val fileContents =
      loadResourceString("integration/enums/TestEnumConfig_1.yml")

    val deserializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[DeserializationFunction[_]],
            classOf[TestEnumConfig]
          )
        )
      )
      .asInstanceOf[Key[DeserializationFunction[TestEnumConfig]]]

    val serializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[SerializationFunction[_]],
            classOf[TestEnumConfig]
          )
        )
      )
      .asInstanceOf[Key[SerializationFunction[TestEnumConfig]]]

    val config = injector
      .getInstance(classOf[ConfigProviderFactory])
      .createStringReaderProvider(
        injector.getInstance(classOf[YamlFileType]),
        fileContents,
        new Configuration(
          null,
          classOf[TestEnumConfig],
          classOf[TestEnumConfigImpl]
        ),
        injector.getInstance(deserializerKey),
        injector.getInstance(serializerKey)
      )
      .getOrThrow()
      .get()

    assertThat(config).isNotNull()
    config.testEnum() shouldBe TestEnum.HELLO
    config.testEnumInexact() shouldBe TestEnum.WORLD
  }

  test("testEnumsCascading") {
    val fileContents =
      loadResourceString("integration/enums/TestEnumConfig_1.yml")

    val deserializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[DeserializationFunction[_]],
            classOf[TestEnumCascadeConfig]
          )
        )
      )
      .asInstanceOf[Key[DeserializationFunction[TestEnumCascadeConfig]]]

    val serializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[SerializationFunction[_]],
            classOf[TestEnumCascadeConfig]
          )
        )
      )
      .asInstanceOf[Key[SerializationFunction[TestEnumCascadeConfig]]]

    val config = injector
      .getInstance(classOf[ConfigProviderFactory])
      .createStringReaderProvider(
        injector.getInstance(classOf[YamlFileType]),
        fileContents,
        new Configuration(
          null,
          classOf[TestEnumCascadeConfig],
          classOf[TestEnumCascadeConfigImpl]
        ),
        injector.getInstance(deserializerKey),
        injector.getInstance(serializerKey)
      )
      .getOrThrow()
      .get()

    assertThat(config).isNotNull()
    config.testEnum() shouldBe TestEnum.HELLO
    config.testEnumInexact() shouldBe TestEnum.WORLD
  }

  test("testEnumsInvalid") {
    val fileContents =
      loadResourceString("integration/enums/TestEnumConfig_invalid.yml")

    val deserializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[DeserializationFunction[_]],
            classOf[TestEnumConfig]
          )
        )
      )
      .asInstanceOf[Key[DeserializationFunction[TestEnumConfig]]]

    val serializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[SerializationFunction[_]],
            classOf[TestEnumConfig]
          )
        )
      )
      .asInstanceOf[Key[SerializationFunction[TestEnumConfig]]]

    val provider = injector
      .getInstance(classOf[ConfigProviderFactory])
      .createStringReaderProvider(
        injector.getInstance(classOf[YamlFileType]),
        fileContents,
        new Configuration(
          null,
          classOf[TestEnumConfig],
          classOf[TestEnumConfigImpl]
        ),
        injector.getInstance(deserializerKey),
        injector.getInstance(serializerKey)
      )
      .getOrThrow()

    intercept[InvalidEnumValueException] {
      provider.get()
    }
  }

  test("testEnumSerialization") {
    val fileContents =
      loadResourceString("integration/enums/TestEnumConfig_1.yml")

    val deserializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[DeserializationFunction[_]],
            classOf[TestEnumConfig]
          )
        )
      )
      .asInstanceOf[Key[DeserializationFunction[TestEnumConfig]]]

    val serializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[SerializationFunction[_]],
            classOf[TestEnumConfig]
          )
        )
      )
      .asInstanceOf[Key[SerializationFunction[TestEnumConfig]]]

    val provider = injector
      .getInstance(classOf[ConfigProviderFactory])
      .createStringReaderProvider(
        injector.getInstance(classOf[YamlFileType]),
        fileContents,
        new Configuration(
          null,
          classOf[TestEnumConfig],
          classOf[TestEnumConfigImpl]
        ),
        injector.getInstance(deserializerKey),
        injector.getInstance(serializerKey)
      )
      .getOrThrow()

    val config = provider.get()

    val saver = injector.getInstance(serializerKey)
    val serialized = saver.apply(
      config,
      new SerializationContext(injector.getInstance(classOf[ObjectMapper]))
    )

    serialized shouldBe a[DataTree.DataTreeMap]
    val map = serialized.asInstanceOf[DataTree.DataTreeMap]

    map.get("testEnum") shouldBe DataTree.string("HELLO")
    map.get("testEnumInexact") shouldBe DataTree.string("WORLD")
  }
}
