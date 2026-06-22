package me.bristermitten.mittenlib.annotations.compile

import com.google.inject.Guice
import com.google.testing.compile.Compilation
import com.google.testing.compile.CompilationSubject.assertThat as assertCompilation
import com.google.testing.compile.Compiler.javac
import com.google.testing.compile.JavaFileObjects
import com.palantir.javapoet.ClassName
import io.toolisticon.aptk.common.ToolingProvider
import io.toolisticon.cute.Cute
import javax.lang.model.element.TypeElement
import me.bristermitten.mittenlib.annotations.config.ConfigProcessor
import me.bristermitten.mittenlib.annotations.domain.ConfigStructure
import me.bristermitten.mittenlib.annotations.parser.ConfigParser
import org.assertj.core.api.Assertions.assertThat
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class ConfigurationClassNameGeneratorTest extends AnyFunSuite with Matchers {

  test("generateFullConfigClassName") {
    val compilation: Compilation = javac()
      .withProcessors(new ConfigProcessor())
      .compile(
        JavaFileObjects.forSourceString(
          "me.bristermitten.mittenlib.tests.LangConfigDTO",
          """
          |package me.bristermitten.mittenlib.tests;
          |
          |import me.bristermitten.mittenlib.config.Config;
          |import me.bristermitten.mittenlib.config.Source;
          |import me.bristermitten.mittenlib.config.extension.UseObjectMapperSerialization;
          |import me.bristermitten.mittenlib.config.names.NamingPattern;
          |import me.bristermitten.mittenlib.lang.LangMessage;
          |
          |@NamingPattern(value = me.bristermitten.mittenlib.config.names.NamingPatterns.LOWER_KEBAB_CASE)
          |@Source(value = "lang.yml")
          |@Config
          |@UseObjectMapperSerialization
          |public class LangConfigDTO {
          |    public final ErrorsDTO errors = null;
          |    public final CommandsDTO commands = null;
          |
          |    @Config
          |    public static class CommandsDTO {
          |        public final SelectionDTO selection = null;
          |
          |        @Config
          |        public static class SelectionDTO {
          |            public final LangMessage rename = null;
          |            public final LangMessage created = null;
          |            public final LangMessage deleted = null;
          |            public final LangMessage addedZone = null;
          |            public final LangMessage removedZone = null;
          |        }
          |    }
          |
          |    @Config
          |    public static class ErrorsDTO {
          |        public final LangMessage noSelection = null;
          |        public final SelectionDTO selection = null;
          |
          |        @Config
          |        public static class SelectionDTO {
          |            public final LangMessage nodeExists = null;
          |            public final LangMessage alreadyHaveSelection = null;
          |            public final LangMessage duplicateZone = null;
          |            public final LangMessage zoneNotPresent = null;
          |        }
          |    }
          |
          |}
          |""".stripMargin
        )
      )

    assertCompilation(compilation).succeeded()
  }

  test("testTranslateConfigClassName") {
    val dtoName = ClassName.bestGuess("TestConfigDTO")
    assertThat(
      ConfigurationClassNameGenerator.translateConfigClassName(dtoName)
    )
      .isEqualTo(ClassName.bestGuess("TestConfig"))

    val configName = ClassName.bestGuess("TestConfig")
    assertThat(
      ConfigurationClassNameGenerator.translateConfigClassName(configName)
    )
      .isEqualTo(ClassName.bestGuess("TestConfigImpl"))
  }

  test("getDeserializerClassName") {
    Cute
      .unitTest()
      .when()
      .passInElement()
      .fromSourceString[TypeElement](
        "me.bristermitten.mittenlib.tests.TestConfig",
        """
          |package me.bristermitten.mittenlib.tests;
          |import me.bristermitten.mittenlib.config.Config;
          |import io.toolisticon.cute.PassIn;
          |@Config
          |@PassIn
          |public class TestConfig {}
          |""".stripMargin
      )
      .intoUnitTest((processingEnvironment, element) => {
        ToolingProvider.setTooling(processingEnvironment)
        val injector =
          Guice.createInjector(new ConfigProcessorModule(processingEnvironment))
        val generator =
          injector.getInstance(classOf[ConfigurationClassNameGenerator])
        val parser = injector.getInstance(classOf[ConfigParser])
        val ast: ConfigStructure = parser.getParsedStructure(element)

        assertThat(generator.getDeserializerClassName(ast))
          .isEqualTo(
            ClassName.bestGuess(
              "me.bristermitten.mittenlib.tests.TestConfigDeserializer"
            )
          )
      })
      .thenExpectThat()
      .compilationSucceeds()
      .executeTest()
  }
}
