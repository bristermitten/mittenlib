package me.bristermitten.mittenlib.annotations.parser

import com.google.inject.Guice
import io.toolisticon.aptk.common.ToolingProvider
import io.toolisticon.cute.Cute
import io.toolisticon.cute.PassIn
import javax.lang.model.element.TypeElement
import me.bristermitten.mittenlib.annotations.compile.ConfigProcessorModule
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class ConfigClassParserCoverageTest extends AnyFunSuite with Matchers {

  test("testParsingMissingConfigAnnotationThrowsException") {
    Cute
      .unitTest()
      .when()
      .passInElement()
      .fromClass[TypeElement](
        classOf[ConfigClassParserCoverageTest.MissingConfigDTO]
      )
      .intoUnitTest((processingEnvironment, element) => {
        ToolingProvider.setTooling(processingEnvironment)
        val injector =
          Guice.createInjector(new ConfigProcessorModule(processingEnvironment))
        val parser = injector.getInstance(classOf[ConfigParser])
        try {
          parser.parseAbstract(element)
          fail("Expected IllegalStateException")
        } catch {
          case _: IllegalStateException => // Expected
        }
      })
      .thenExpectThat()
      .compilationFails()
      .andThat()
      .compilerMessage()
      .ofKindError()
      .contains("does not have a @Config annotation")
      .executeTest()
  }
}

object ConfigClassParserCoverageTest {
  @PassIn
  class MissingConfigDTO {
    var x: Int = 0
  }
}
