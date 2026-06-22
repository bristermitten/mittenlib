package me.bristermitten.mittenlib.annotations.parser

import io.toolisticon.aptk.common.ToolingProvider
import io.toolisticon.cute.Cute
import javax.lang.model.element.TypeElement
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class CustomDeserializersTest extends AnyFunSuite with Matchers {

  test("testRegisterCustomDeserializerMissingAnnotationReportsError") {
    Cute
      .unitTest()
      .when()
      .passInElement()
      .fromSourceString[TypeElement](
        "me.bristermitten.mittenlib.annotations.parser.MissingAnnotationDeserializer",
        """
          |package me.bristermitten.mittenlib.annotations.parser;
          |import io.toolisticon.cute.PassIn;
          |@PassIn
          |class MissingAnnotationDeserializer {}
          |""".stripMargin
      )
      .intoUnitTest((processingEnvironment, element) => {
        ToolingProvider.setTooling(processingEnvironment)
        val registry = new CustomDeserializers()
        registry.registerCustomDeserializer(element)
      })
      .thenExpectThat()
      .compilationFails()
      .executeTest()
  }

  test("testRegisterCustomDeserializerInvalidInterfaceOrMethodReportsError") {
    Cute
      .unitTest()
      .when()
      .passInElement()
      .fromSourceString[TypeElement](
        "me.bristermitten.mittenlib.annotations.parser.InvalidDeserializer",
        """
          |package me.bristermitten.mittenlib.annotations.parser;
          |import io.toolisticon.cute.PassIn;
          |import me.bristermitten.mittenlib.config.extension.CustomDeserializerFor;
          |@CustomDeserializerFor(String.class)
          |@PassIn
          |class InvalidDeserializer {}
          |""".stripMargin
      )
      .intoUnitTest((processingEnvironment, element) => {
        ToolingProvider.setTooling(processingEnvironment)
        val registry = new CustomDeserializers()
        registry.registerCustomDeserializer(element)
      })
      .thenExpectThat()
      .compilationFails()
      .executeTest()
  }
}
