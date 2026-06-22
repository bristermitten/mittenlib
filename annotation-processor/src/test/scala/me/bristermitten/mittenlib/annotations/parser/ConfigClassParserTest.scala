package me.bristermitten.mittenlib.annotations.parser

import com.google.inject.Guice
import com.palantir.javapoet.ClassName
import io.toolisticon.aptk.common.ToolingProvider
import io.toolisticon.cute.Cute
import javax.lang.model.element.TypeElement
import me.bristermitten.mittenlib.annotations.compile.ConfigImplGenerator
import me.bristermitten.mittenlib.annotations.compile.ConfigProcessorModule
import me.bristermitten.mittenlib.annotations.domain.ConfigStructure
import me.bristermitten.mittenlib.annotations.domain.Property
import me.bristermitten.mittenlib.annotations.integration.AtomicConfig
import me.bristermitten.mittenlib.annotations.integration.InterfaceConfig
import me.bristermitten.mittenlib.annotations.integration.IntersectionConfig
import me.bristermitten.mittenlib.annotations.integration.UnionConfig
import org.assertj.core.api.Assertions.assertThat
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import scala.jdk.CollectionConverters.*

class ConfigClassParserTest extends AnyFunSuite with Matchers {

  test("testParsingAtomicConfig") {
    Cute
      .unitTest()
      .when()
      .passInElement()
      .fromClass[TypeElement](classOf[AtomicConfig])
      .intoUnitTest((processingEnvironment, element) => {
        val injector =
          Guice.createInjector(new ConfigProcessorModule(processingEnvironment))
        val ast = injector
          .getInstance(classOf[ConfigParser])
          .getParsedStructure(element)

        assertThat(ast).isNotNull()
        ast shouldBe a[ConfigStructure.Atomic]
      })
      .thenExpectThat()
      .compilationSucceeds()
      .executeTest()
  }

  test("testParsingIntersectionConfig") {
    Cute
      .unitTest()
      .when()
      .passInElement()
      .fromClass[TypeElement](classOf[IntersectionConfig])
      .intoUnitTest((processingEnvironment, element) => {
        val injector =
          Guice.createInjector(new ConfigProcessorModule(processingEnvironment))
        val ast = injector
          .getInstance(classOf[ConfigParser])
          .getParsedStructure(element)

        assertThat(ast).isNotNull()
        ast shouldBe a[ConfigStructure.Atomic]

        val enclosed = ast.enclosed.asJava
        assertThat(enclosed).hasSize(1)
        val firstEnclosed = enclosed.get(0)
        firstEnclosed shouldBe a[ConfigStructure.Intersection]
        assertThat(firstEnclosed.name).isEqualTo(
          ClassName.get(classOf[IntersectionConfig.ChildIntersectionConfig])
        )
      })
      .thenExpectThat()
      .compilationSucceeds()
      .executeTest()
  }

  test("testParsingNestedConfig") {
    Cute
      .unitTest()
      .when()
      .passInElement()
      .fromClass[TypeElement](classOf[InterfaceConfig])
      .intoUnitTest((processingEnvironment, element) => {
        val injector =
          Guice.createInjector(new ConfigProcessorModule(processingEnvironment))
        val ast = injector
          .getInstance(classOf[ConfigParser])
          .getParsedStructure(element)

        assertThat(ast).isNotNull()
        ast shouldBe a[ConfigStructure.Atomic]

        val enclosed = ast.enclosed.asJava
        assertThat(enclosed).hasSize(1)
        enclosed.get(0) shouldBe a[ConfigStructure.Atomic]
      })
      .thenExpectThat()
      .compilationSucceeds()
      .executeTest()
  }

  test("testParsingUnionConfig") {
    Cute
      .unitTest()
      .when()
      .passInElement()
      .fromClass[TypeElement](classOf[UnionConfig])
      .intoUnitTest((processingEnvironment, element) => {
        val injector =
          Guice.createInjector(new ConfigProcessorModule(processingEnvironment))
        val ast = injector
          .getInstance(classOf[ConfigParser])
          .getParsedStructure(element)

        assertThat(ast).isNotNull()
        ast shouldBe a[ConfigStructure.Union]
        val union = ast.asInstanceOf[ConfigStructure.Union]

        val alternatives = union.alternatives.asJava
        assertThat(alternatives).hasSize(2)
        val firstAlternative = alternatives.get(0)
        firstAlternative shouldBe a[ConfigStructure.Intersection]
      })
      .thenExpectThat()
      .compilationSucceeds()
      .executeTest()
  }

  test("testParsingBasicConfig") {
    Cute
      .unitTest()
      .when()
      .passInElement()
      .fromSourceString[TypeElement](
        "me.bristermitten.mittenlib.tests.TestInterfaceConfig",
        """
          |package me.bristermitten.mittenlib.tests;
          |import io.toolisticon.cute.PassIn;
          |import me.bristermitten.mittenlib.config.Config;
          |import me.bristermitten.mittenlib.config.names.ConfigName;
          |
          |@Config
          |@PassIn
          |public interface TestInterfaceConfig {
          |    @ConfigName("thing-name")
          |    String name();
          |
          |    int age();
          |}
          |""".stripMargin
      )
      .intoUnitTest((processingEnvironment, element) => {
        ToolingProvider.setTooling(processingEnvironment)
        val injector =
          Guice.createInjector(new ConfigProcessorModule(processingEnvironment))
        val ast = injector
          .getInstance(classOf[ConfigParser])
          .getParsedStructure(element)

        assertThat(ast.name.simpleName).isEqualTo("TestInterfaceConfig")

        val properties = ast.properties.asJava
        assertThat(properties).hasSize(2)

        val firstProp = properties.get(0)
        assertThat(firstProp.name).isEqualTo("name")
        assertThat(firstProp.configName.get).isEqualTo("thing-name")

        val generator = injector.getInstance(classOf[ConfigImplGenerator])
        val emit = generator.emit(ast)

        assertThat(emit).isNotNull()
        assertThat(emit.typeSpec().name()).isEqualTo("TestInterfaceConfigImpl")
      })
      .thenExpectThat()
      .compilationSucceeds()
      .executeTest()
  }

  test("testParsingConfigWithMultipleParents") {
    Cute
      .unitTest()
      .when()
      .passInElement()
      .fromSourceString[TypeElement](
        "me.bristermitten.mittenlib.tests.MultiParentConfigDTO",
        """
          |package me.bristermitten.mittenlib.tests;
          |import io.toolisticon.cute.PassIn;
          |import me.bristermitten.mittenlib.config.Config;
          |
          |interface SomeInterface {}
          |class SuperClass {}
          |
          |@Config
          |@PassIn
          |public class MultiParentConfigDTO extends SuperClass implements SomeInterface {
          |    public int level;
          |}
          |""".stripMargin
      )
      .intoUnitTest((processingEnvironment, element) => {
        val injector =
          Guice.createInjector(new ConfigProcessorModule(processingEnvironment))
        val parser = injector.getInstance(classOf[ConfigParser])
        intercept[IllegalArgumentException] {
          parser.getParsedStructure(element)
        }
      })
      .thenExpectThat()
      .compilationSucceeds()
      .executeTest()
  }

  test("testParsingConfigEnum") {
    Cute
      .unitTest()
      .when()
      .passInElement()
      .fromSourceString[TypeElement](
        "me.bristermitten.mittenlib.tests.EnumConfigDTO",
        """
          |package me.bristermitten.mittenlib.tests;
          |import io.toolisticon.cute.PassIn;
          |import me.bristermitten.mittenlib.config.Config;
          |
          |@Config
          |@PassIn
          |public enum EnumConfigDTO {
          |    VAL
          |}
          |""".stripMargin
      )
      .intoUnitTest((processingEnvironment, element) => {
        val injector =
          Guice.createInjector(new ConfigProcessorModule(processingEnvironment))
        val parser = injector.getInstance(classOf[ConfigParser])
        intercept[IllegalArgumentException] {
          parser.getParsedStructure(element)
        }
      })
      .thenExpectThat()
      .compilationSucceeds()
      .executeTest()
  }
}
