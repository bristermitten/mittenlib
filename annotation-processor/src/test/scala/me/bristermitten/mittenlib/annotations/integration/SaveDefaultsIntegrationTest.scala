package me.bristermitten.mittenlib.annotations.integration

import com.google.inject.{AbstractModule, Guice, Injector, Key, TypeLiteral}
import com.google.inject.util.Types
import java.io.IOException
import java.nio.file.{Files, NoSuchFileException, Path}
import java.util.List
import me.bristermitten.mittenlib.MittenLibConsumer
import me.bristermitten.mittenlib.annotations.util.IntegrationTests.loadResourceString
import me.bristermitten.mittenlib.config.{
  Configuration,
  DeserializationFunction,
  SerializationContext,
  SerializationFunction
}
import me.bristermitten.mittenlib.config.paths.PluginConfigInitializationStrategy
import me.bristermitten.mittenlib.config.provider.{
  ConfigProvider,
  FileBasedConfigProvider,
  SaveableConfigProvider
}
import me.bristermitten.mittenlib.config.provider.construct.ConfigProviderFactory
import me.bristermitten.mittenlib.config.reader.{ConfigReader, ObjectMapper}
import me.bristermitten.mittenlib.config.tree.DataTree
import me.bristermitten.mittenlib.config.writer.ConfigWriter
import me.bristermitten.mittenlib.files.FileTypeModule
import me.bristermitten.mittenlib.files.yaml.{YamlFileType, YamlObjectWriter}
import me.bristermitten.mittenlib.watcher.FileWatcherModule
import org.assertj.core.api.Assertions.assertThat
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfterEach, Outcome}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.bukkit.plugin.Plugin

class SaveDefaultsIntegrationTest
    extends AnyFunSuite
    with Matchers
    with BeforeAndAfterEach {

  private val mockPlugin = mock(classOf[Plugin])
  private var tempDir: Path = _
  private var injector: Injector = _

  override def beforeEach(): Unit = {
    tempDir = Files.createTempDirectory("mittenlib-temp")
    when(mockPlugin.getDataFolder).thenReturn(tempDir.toFile)
    when(mockPlugin.getName).thenReturn("TestPlugin")

    injector = Guice.createInjector(
      new ConfigLoaderModule().asModuleWithInfrastructure(),
      new FileWatcherModule(),
      new FileTypeModule(),
      new AbstractModule() {
        override def configure(): Unit = {
          bind(classOf[Plugin]).toInstance(mockPlugin)
          bind(classOf[MittenLibConsumer]).toInstance(
            new MittenLibConsumer("Tests")
          )
        }
      }
    )
  }

  override def afterEach(): Unit = {
    // Delete tempDir contents and the directory itself
    if (tempDir != null && Files.exists(tempDir)) {
      import scala.jdk.CollectionConverters.*
      Files
        .walk(tempDir)
        .sorted(java.util.Comparator.reverseOrder())
        .forEach(p => Files.delete(p))
    }
  }

  test("testSerializeClassConfig") {
    val fileContents =
      loadResourceString("integration/InterfaceConfig_dummy.yml")

    val loaderKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[DeserializationFunction[_]],
            classOf[ClassConfigImpl]
          )
        )
      )
      .asInstanceOf[Key[DeserializationFunction[ClassConfigImpl]]]

    val saverKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[SerializationFunction[_]],
            classOf[ClassConfigImpl]
          )
        )
      )
      .asInstanceOf[Key[SerializationFunction[ClassConfigImpl]]]

    val loader = injector.getInstance(loaderKey)
    val saverFunc = injector.getInstance(saverKey)

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
        loader,
        saverFunc
      )
      .getOrThrow()

    val classConfig = stringReaderProvider.get()

    // Verify default value was applied
    classConfig.defaultValue() shouldBe 1

    // Serialize the config back to a DataTree
    val saver = injector.getInstance(saverKey)
    val serialized = saver.apply(
      classConfig,
      new SerializationContext(injector.getInstance(classOf[ObjectMapper]))
    )

    // Verify the serialized data contains all fields including the default
    serialized shouldBe a[DataTree.DataTreeMap]
    val map = serialized.asInstanceOf[DataTree.DataTreeMap]

    val defaultValueTree = map.get("defaultValue")
    defaultValueTree should not be null
    defaultValueTree shouldBe a[DataTree.DataTreeLiteral.DataTreeLiteralInt]
    defaultValueTree
      .asInstanceOf[DataTree.DataTreeLiteral.DataTreeLiteralInt]
      .value shouldBe 1
  }

  test("testSaveOnlyMissingFields") {
    val originalContent =
      """
        |age: 3
        |thing-name: a
        |children: []
        |""".stripMargin

    val configFile = tempDir.resolve("test-config.yml")
    Files.writeString(configFile, originalContent)

    val reader = injector.getInstance(classOf[ConfigReader])
    val writer = injector.getInstance(classOf[YamlObjectWriter])
    val saver = injector.getInstance(classOf[ConfigWriter])

    val loaderKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[DeserializationFunction[_]],
            classOf[ClassConfigImpl]
          )
        )
      )
      .asInstanceOf[Key[DeserializationFunction[ClassConfigImpl]]]

    val saverKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[SerializationFunction[_]],
            classOf[ClassConfigImpl]
          )
        )
      )
      .asInstanceOf[Key[SerializationFunction[ClassConfigImpl]]]

    val loader = injector.getInstance(loaderKey)
    val saverFunc = injector.getInstance(saverKey)

    val provider = new FileBasedConfigProvider[ClassConfigImpl](
      configFile,
      reader,
      loader,
      saver,
      saverFunc,
      writer
    )

    val classConfig = provider.get()
    classConfig.defaultValue() shouldBe 1
    classConfig.age() shouldBe 3
    classConfig.name() shouldBe "a"

    provider.save(classConfig).getOrThrow()

    val savedContent = Files.readString(configFile)
    savedContent should include("defaultValue: 1")
    savedContent should include("age: 3")
    savedContent should include("thing-name: a")
  }

  test("testSaveWithOverride") {
    val originalContent =
      """
        |age: 5
        |thing-name: original
        |children: []
        |""".stripMargin

    val configFile = tempDir.resolve("test-config-override.yml")
    Files.writeString(configFile, originalContent)

    val reader = injector.getInstance(classOf[ConfigReader])
    val writer = injector.getInstance(classOf[YamlObjectWriter])
    val saver = injector.getInstance(classOf[ConfigWriter])

    val loaderKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[DeserializationFunction[_]],
            classOf[ClassConfigImpl]
          )
        )
      )
      .asInstanceOf[Key[DeserializationFunction[ClassConfigImpl]]]

    val saverKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[SerializationFunction[_]],
            classOf[ClassConfigImpl]
          )
        )
      )
      .asInstanceOf[Key[SerializationFunction[ClassConfigImpl]]]

    val loader = injector.getInstance(loaderKey)
    val saverFunc = injector.getInstance(saverKey)

    val provider = new FileBasedConfigProvider[ClassConfigImpl](
      configFile,
      reader,
      loader,
      saver,
      saverFunc,
      writer
    )

    val classConfig = provider.get()
    classConfig.age() shouldBe 5

    val modifiedConfig = classConfig.withAge(10).withName("modified")

    provider.save(modifiedConfig, true).getOrThrow()

    val savedContent = Files.readString(configFile)
    savedContent should include("age: 10")
    savedContent should include("thing-name: modified")
    savedContent should include("defaultValue: 1")
  }

  test("testSavePreservesExistingFields") {
    val originalContent =
      """
        |age: 7
        |thing-name: existing
        |defaultValue: 99
        |children: []
        |""".stripMargin

    val configFile = tempDir.resolve("test-config-preserve.yml")
    Files.writeString(configFile, originalContent)

    val reader = injector.getInstance(classOf[ConfigReader])
    val writer = injector.getInstance(classOf[YamlObjectWriter])
    val saver = injector.getInstance(classOf[ConfigWriter])

    val loaderKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[DeserializationFunction[_]],
            classOf[ClassConfigImpl]
          )
        )
      )
      .asInstanceOf[Key[DeserializationFunction[ClassConfigImpl]]]

    val saverKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[SerializationFunction[_]],
            classOf[ClassConfigImpl]
          )
        )
      )
      .asInstanceOf[Key[SerializationFunction[ClassConfigImpl]]]

    val loader = injector.getInstance(loaderKey)
    val saverFunc = injector.getInstance(saverKey)

    val provider = new FileBasedConfigProvider[ClassConfigImpl](
      configFile,
      reader,
      loader,
      saver,
      saverFunc,
      writer
    )

    val classConfig = provider.get()
    classConfig.defaultValue() shouldBe 99

    provider.save(classConfig, false).getOrThrow()

    val savedContent = Files.readString(configFile)
    savedContent should include("defaultValue: 99")
    savedContent should include("age: 7")
    savedContent should include("thing-name: existing")
  }

  test("testSaveCreatesNewFileIfNotExists") {
    val configFile = tempDir.resolve("new-config.yml")
    Files.exists(configFile) shouldBe false

    val reader = injector.getInstance(classOf[ConfigReader])
    val writer = injector.getInstance(classOf[YamlObjectWriter])
    val saver = injector.getInstance(classOf[ConfigWriter])

    val loaderKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[DeserializationFunction[_]],
            classOf[ClassConfigImpl]
          )
        )
      )
      .asInstanceOf[Key[DeserializationFunction[ClassConfigImpl]]]

    val saverKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[SerializationFunction[_]],
            classOf[ClassConfigImpl]
          )
        )
      )
      .asInstanceOf[Key[SerializationFunction[ClassConfigImpl]]]

    val loader = injector.getInstance(loaderKey)
    val saverFunc = injector.getInstance(saverKey)

    val provider = new FileBasedConfigProvider[ClassConfigImpl](
      configFile,
      reader,
      loader,
      saver,
      saverFunc,
      writer
    )

    val classConfig = new ClassConfigImpl("test", 42, 1, List.of(), null)

    provider.save(classConfig, false).getOrThrow()

    Files.exists(configFile) shouldBe true

    val savedContent = Files.readString(configFile)
    savedContent should include("age: 42")
    savedContent should include("thing-name: test")
    savedContent should include("defaultValue: 1")
  }

  test("testGenerateDefaultWhenFileMissing") {
    val configFile = tempDir.resolve("generated-default-config.yml")
    Files.exists(configFile) shouldBe false

    val reader = injector.getInstance(classOf[ConfigReader])
    val writer = injector.getInstance(classOf[YamlObjectWriter])
    val saver = injector.getInstance(classOf[ConfigWriter])

    val loaderKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[DeserializationFunction[_]],
            classOf[FullyDefaultConfigImpl]
          )
        )
      )
      .asInstanceOf[Key[DeserializationFunction[FullyDefaultConfigImpl]]]

    val saverKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[SerializationFunction[_]],
            classOf[FullyDefaultConfigImpl]
          )
        )
      )
      .asInstanceOf[Key[SerializationFunction[FullyDefaultConfigImpl]]]

    val loader = injector.getInstance(loaderKey)
    val saverFunc = injector.getInstance(saverKey)

    val provider = new FileBasedConfigProvider[FullyDefaultConfigImpl](
      configFile,
      reader,
      loader,
      saver,
      saverFunc,
      writer
    )

    val config = provider.get()

    Files.exists(configFile) shouldBe true
    config.age() shouldBe 42
    config.name() shouldBe "default"

    val savedContent = Files.readString(configFile)
    savedContent should include("age: 42")
    savedContent should include("name: default")
  }

  test("testDenyMissingResourceForNonDynamicConfig") {
    val configFile = tempDir.resolve("non-existent-config.yml")

    val reader = injector.getInstance(classOf[ConfigReader])
    val writer = injector.getInstance(classOf[YamlObjectWriter])
    val saver = injector.getInstance(classOf[ConfigWriter])

    val loaderKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[DeserializationFunction[_]],
            classOf[ClassConfigImpl]
          )
        )
      )
      .asInstanceOf[Key[DeserializationFunction[ClassConfigImpl]]]

    val saverKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[SerializationFunction[_]],
            classOf[ClassConfigImpl]
          )
        )
      )
      .asInstanceOf[Key[SerializationFunction[ClassConfigImpl]]]

    val loader = injector.getInstance(loaderKey)
    val saverFunc = injector.getInstance(saverKey)

    val provider = new FileBasedConfigProvider[ClassConfigImpl](
      configFile,
      reader,
      loader,
      saver,
      saverFunc,
      writer
    )

    intercept[NoSuchFileException] {
      provider.get()
    }
    Files.exists(configFile) shouldBe false
  }

  test("testInformativeErrorMessageWhenDynamicInitializationFails") {
    val strategy =
      injector.getInstance(classOf[PluginConfigInitializationStrategy])
    val result = strategy.initializeConfig(
      "non-existent-config.yml",
      classOf[ClassConfigImpl]
    )

    result.isFailure shouldBe true
    val message = result.error().get().getMessage
    message should include("Could not find resource non-existent-config.yml")
    message should include("is not dynamically initializable")
    message should include(
      "following required properties lack default values: name, age, children"
    )
    message should include(
      "Either provide a default config file in your jar's resources"
    )
  }

  test("testFeaturesConfigDefaultValueSerialization") {
    val saverKey = Key
      .get(
        TypeLiteral.get(
          Types.newParameterizedType(
            classOf[SerializationFunction[_]],
            classOf[FeaturesConfig]
          )
        )
      )
      .asInstanceOf[Key[SerializationFunction[FeaturesConfig]]]

    val saver = injector.getInstance(saverKey)

    val defaultValue = saver.generateDefault(
      new SerializationContext(injector.getInstance(classOf[ObjectMapper]))
    )

    defaultValue should not be null
    defaultValue shouldBe a[DataTree.DataTreeMap]
    val map = defaultValue.asInstanceOf[DataTree.DataTreeMap]

    val flagsTree = map.get("flags")
    flagsTree should not be null
    flagsTree shouldBe a[DataTree.DataTreeMap]
    flagsTree.asInstanceOf[DataTree.DataTreeMap].values().isEmpty shouldBe true

    val flagsWithDefaultsTree = map.get("flagsWithDefaults")
    flagsWithDefaultsTree should not be null
    flagsWithDefaultsTree shouldBe a[DataTree.DataTreeMap]

    val innerMap =
      flagsWithDefaultsTree.asInstanceOf[DataTree.DataTreeMap].values()
    innerMap should have size 2
    innerMap.get(DataTree.string("a")) shouldBe DataTree.bool(true)
    innerMap.get(DataTree.string("b")) shouldBe DataTree.bool(false)
  }

  test("testGuiceBindingsForSaveableConfigProvider") {
    val saveableProvider = injector.getInstance(
      Key.get(new TypeLiteral[SaveableConfigProvider[UnionConfig]]() {})
    )
    saveableProvider should not be null

    val saveableImplProvider = injector.getInstance(
      Key.get(new TypeLiteral[SaveableConfigProvider[UnionConfigImpl]]() {})
    )
    saveableImplProvider should not be null

    val configProvider = injector.getInstance(
      Key.get(new TypeLiteral[ConfigProvider[UnionConfig]]() {})
    )
    configProvider should not be null

    val configImplProvider = injector.getInstance(
      Key.get(new TypeLiteral[ConfigProvider[UnionConfigImpl]]() {})
    )
    configImplProvider should not be null

    injector.getProvider(classOf[UnionConfig]) should not be null
    injector.getProvider(classOf[UnionConfigImpl]) should not be null
  }
}
