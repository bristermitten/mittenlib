package me.bristermitten.mittenlib.annotations.integration.extension

import com.google.inject.{AbstractModule, Guice, Injector, Key, TypeLiteral}
import com.google.inject.util.Types
import me.bristermitten.mittenlib.MittenLibConsumer
import me.bristermitten.mittenlib.annotations.integration.ConfigLoaderModule
import me.bristermitten.mittenlib.annotations.integration.extension.fallback.{
  CustomTypeFallback,
  CustomTypeFallbackConfig,
  CustomTypeFallbackConfigImpl
}
import me.bristermitten.mittenlib.config.{
  ConfigInfrastructureModule,
  Configuration,
  DeserializationFunction,
  SerializationFunction
}
import me.bristermitten.mittenlib.config.provider.construct.ConfigProviderFactory
import me.bristermitten.mittenlib.files.FileTypeModule
import me.bristermitten.mittenlib.files.yaml.YamlFileType
import me.bristermitten.mittenlib.watcher.FileWatcherModule
import org.assertj.core.api.Assertions.assertThat
import org.scalatest.BeforeAndAfterEach
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class CustomDeserializerIntegrationTest
    extends AnyFunSuite
    with Matchers
    with BeforeAndAfterEach {

  private var injector: Injector = _

  override def beforeEach(): Unit = {
    injector = Guice.createInjector(
      new ConfigInfrastructureModule(),
      new ConfigLoaderModule().asModule(),
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

  test("test") {
    val deserializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[DeserializationFunction[_]],
            classOf[CustomTypeConfig]
          )
        )
      )
      .asInstanceOf[Key[DeserializationFunction[CustomTypeConfig]]]

    val serializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[SerializationFunction[_]],
            classOf[CustomTypeConfig]
          )
        )
      )
      .asInstanceOf[Key[SerializationFunction[CustomTypeConfig]]]

    val stringReaderProvider = injector
      .getInstance(classOf[ConfigProviderFactory])
      .createStringReaderProvider(
        injector.getInstance(classOf[YamlFileType]),
        """
          |customType: 'blahblah'
          |customTypes: [ 'f' ]
          |""".stripMargin,
        new Configuration(
          null,
          classOf[CustomTypeConfig],
          classOf[CustomTypeConfigImpl]
        ),
        injector.getInstance(deserializerKey),
        injector.getInstance(serializerKey)
      )
      .getOrThrow()

    val customTypeConfig = stringReaderProvider.get()
    customTypeConfig.customType() shouldBe new CustomType("hello")

    import scala.jdk.CollectionConverters.*
    val list = customTypeConfig.customTypes().asScala.toList
    list should have size 1
    list.head shouldBe new CustomType("hello")
  }

  test("testFallback") {
    val deserializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[DeserializationFunction[_]],
            classOf[CustomTypeFallbackConfig]
          )
        )
      )
      .asInstanceOf[Key[DeserializationFunction[CustomTypeFallbackConfig]]]

    val serializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[SerializationFunction[_]],
            classOf[CustomTypeFallbackConfig]
          )
        )
      )
      .asInstanceOf[Key[SerializationFunction[CustomTypeFallbackConfig]]]

    val stringReaderProvider = injector
      .getInstance(classOf[ConfigProviderFactory])
      .createStringReaderProvider(
        injector.getInstance(classOf[YamlFileType]),
        "customType: { test: blahblah }",
        new Configuration(
          null,
          classOf[CustomTypeFallbackConfig],
          classOf[CustomTypeFallbackConfigImpl]
        ),
        injector.getInstance(deserializerKey),
        injector.getInstance(serializerKey)
      )
      .getOrThrow()

    val customTypeConfig = stringReaderProvider.get()
    customTypeConfig.customType().test() shouldBe "blahblah"
  }
}
