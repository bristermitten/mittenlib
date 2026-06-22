package me.bristermitten.mittenlib.annotations.integration.extension

import com.google.inject.{AbstractModule, Guice, Injector, Key, TypeLiteral}
import com.google.inject.util.Types
import me.bristermitten.mittenlib.MittenLibConsumer
import me.bristermitten.mittenlib.annotations.integration.ConfigLoaderModule
import me.bristermitten.mittenlib.config.{
  Configuration,
  DeserializationFunction,
  SerializationContext,
  SerializationFunction
}
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

class CustomSerializerIntegrationTest
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
            new MittenLibConsumer("SerializerTests")
          )
        }
      }
    )
  }

  test("testSerialization") {
    val serializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[SerializationFunction[_]],
            classOf[SerializerCustomTypeConfig]
          )
        )
      )
      .asInstanceOf[Key[SerializationFunction[SerializerCustomTypeConfig]]]

    val saver = injector.getInstance(serializerKey)

    val config = new SerializerCustomTypeConfig() {
      override def customType(): SerializerCustomType =
        new SerializerCustomType("test-value")

      override def customTypeList(): java.util.List[SerializerCustomType] =
        java.util.List.of(
          new SerializerCustomType("value-1"),
          new SerializerCustomType("value-2")
        )

      override def nestedCustomTypeList()
          : java.util.List[java.util.List[SerializerCustomType]] =
        java.util.List.of(
          java.util.List.of(new SerializerCustomType("nested-1")),
          java.util.List.of(
            new SerializerCustomType("nested-2"),
            new SerializerCustomType("nested-3")
          )
        )
    }

    val mapper = injector.getInstance(classOf[ObjectMapper])
    val context = new SerializationContext(mapper)
    val result = saver.apply(config, context)

    assertThat(result).isNotNull()
    result.get("customType") shouldBe DataTree.string("serialized-test-value")

    val customTypeListResult = result.get("customTypeList")
    customTypeListResult shouldBe a[DataTree.DataTreeArray]
    val array = customTypeListResult.asInstanceOf[DataTree.DataTreeArray]
    assertThat(array.value()).containsExactly(
      DataTree.string("serialized-value-1"),
      DataTree.string("serialized-value-2")
    )

    val nestedCustomTypeListResult = result.get("nestedCustomTypeList")
    nestedCustomTypeListResult shouldBe a[DataTree.DataTreeArray]
    val outerArray =
      nestedCustomTypeListResult.asInstanceOf[DataTree.DataTreeArray]
    assertThat(outerArray.value()).hasSize(2)

    val innerArray1 =
      outerArray.value().get(0).asInstanceOf[DataTree.DataTreeArray]
    assertThat(innerArray1.value())
      .containsExactly(DataTree.string("serialized-nested-1"))

    val innerArray2 =
      outerArray.value().get(1).asInstanceOf[DataTree.DataTreeArray]
    assertThat(innerArray2.value()).containsExactly(
      DataTree.string("serialized-nested-2"),
      DataTree.string("serialized-nested-3")
    )
  }

  test("testDeserialization") {
    val deserializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[DeserializationFunction[_]],
            classOf[SerializerCustomTypeConfig]
          )
        )
      )
      .asInstanceOf[Key[DeserializationFunction[SerializerCustomTypeConfig]]]

    val serializerKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[SerializationFunction[_]],
            classOf[SerializerCustomTypeConfig]
          )
        )
      )
      .asInstanceOf[Key[SerializationFunction[SerializerCustomTypeConfig]]]

    val stringReaderProvider = injector
      .getInstance(classOf[ConfigProviderFactory])
      .createStringReaderProvider(
        injector.getInstance(classOf[YamlFileType]),
        """
          |customType: 'anything'
          |customTypeList: ['anything1', 'anything2']
          |nestedCustomTypeList: [['anything3'], ['anything4', 'anything5']]
          |""".stripMargin,
        new Configuration(null, classOf[SerializerCustomTypeConfig]),
        injector.getInstance(deserializerKey),
        injector.getInstance(serializerKey)
      )
      .getOrThrow()

    val config = stringReaderProvider.get()
    assertThat(config).isNotNull()
    config.customType() shouldBe new SerializerCustomType("deserialized")

    assertThat(config.customTypeList()).containsExactly(
      new SerializerCustomType("deserialized"),
      new SerializerCustomType("deserialized")
    )

    assertThat(config.nestedCustomTypeList()).containsExactly(
      java.util.List.of(new SerializerCustomType("deserialized")),
      java.util.List.of(
        new SerializerCustomType("deserialized"),
        new SerializerCustomType("deserialized")
      )
    )
  }
}
