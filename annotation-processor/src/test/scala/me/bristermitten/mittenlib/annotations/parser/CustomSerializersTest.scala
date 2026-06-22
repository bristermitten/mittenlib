package me.bristermitten.mittenlib.annotations.parser

import io.toolisticon.aptk.common.ToolingProvider
import io.toolisticon.cute.Cute
import javax.lang.model.element.TypeElement
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class CustomSerializersTest extends AnyFunSuite with Matchers {

  test("testRegisterCustomSerializerMissingAnnotationReportsError") {
    Cute
      .unitTest()
      .when()
      .passInElement()
      .fromSourceString[TypeElement](
        "me.bristermitten.mittenlib.annotations.parser.MissingAnnotationSerializer",
        """
          |package me.bristermitten.mittenlib.annotations.parser;
          |import io.toolisticon.cute.PassIn;
          |@PassIn
          |class MissingAnnotationSerializer {}
          |""".stripMargin
      )
      .intoUnitTest((processingEnvironment, element) => {
        ToolingProvider.setTooling(processingEnvironment)
        val registry = new CustomSerializers()
        registry.registerCustomSerializer(element)
      })
      .thenExpectThat()
      .compilationFails()
      .executeTest()
  }

  test("testRegisterCustomSerializerInvalidInterfaceOrMethodReportsError") {
    Cute
      .unitTest()
      .when()
      .passInElement()
      .fromSourceString[TypeElement](
        "me.bristermitten.mittenlib.annotations.parser.InvalidSerializer",
        """
          |package me.bristermitten.mittenlib.annotations.parser;
          |import io.toolisticon.cute.PassIn;
          |import me.bristermitten.mittenlib.config.extension.CustomSerializerFor;
          |@CustomSerializerFor(String.class)
          |@PassIn
          |class InvalidSerializer {}
          |""".stripMargin
      )
      .intoUnitTest((processingEnvironment, element) => {
        ToolingProvider.setTooling(processingEnvironment)
        val registry = new CustomSerializers()
        registry.registerCustomSerializer(element)
      })
      .thenExpectThat()
      .compilationFails()
      .executeTest()
  }
}
