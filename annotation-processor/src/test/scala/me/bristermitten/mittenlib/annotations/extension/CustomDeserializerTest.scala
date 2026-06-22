package me.bristermitten.mittenlib.annotations.extension

import io.toolisticon.aptk.common.ToolingProvider
import io.toolisticon.cute.Cute
import javax.lang.model.element.TypeElement
import me.bristermitten.mittenlib.annotations.parser.CustomDeserializers
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class CustomDeserializerTest extends AnyFunSuite with Matchers {

  test("testSuccessfulRegistration") {
    Cute
      .unitTest()
      .when()
      .passInElement()
      .fromSourceString[TypeElement](
        "me.bristermitten.mittenlib.annotations.integration.extension.CustomTypeDeserializer",
        """
          |import io.toolisticon.cute.PassIn;import me.bristermitten.mittenlib.annotations.integration.extension.CustomType;
          |import me.bristermitten.mittenlib.config.DeserializationContext;
          |import me.bristermitten.mittenlib.config.extension.CustomDeserializerFor;
          |import me.bristermitten.mittenlib.util.Result;
          |
          |@CustomDeserializerFor(CustomType.class)
          |@PassIn
          |public class CustomTypeDeserializer {
          |
          |    public static Result<CustomType> deserialize(DeserializationContext context) {
          |        return Result.ok(
          |                new CustomType("hello")
          |        );
          |    }
          |}
          |""".stripMargin
      )
      .intoUnitTest((processingEnvironment, element) => {
        ToolingProvider.setTooling(processingEnvironment)
        val customDeserializers = new CustomDeserializers()
        customDeserializers.registerCustomDeserializer(element)
      })
      .thenExpectThat()
      .compilationSucceeds()
      .executeTest()
  }

  test("testNonStaticDeserializerWithoutInterfaceFails") {
    Cute
      .unitTest()
      .when()
      .passInElement()
      .fromSourceString[TypeElement](
        "me.bristermitten.mittenlib.annotations.integration.extension.CustomTypeDeserializer",
        """
          |import io.toolisticon.cute.PassIn;import me.bristermitten.mittenlib.annotations.integration.extension.CustomType;
          |import me.bristermitten.mittenlib.config.DeserializationContext;
          |import me.bristermitten.mittenlib.config.extension.CustomDeserializerFor;
          |import me.bristermitten.mittenlib.util.Result;
          |
          |@CustomDeserializerFor(CustomType.class)
          |@PassIn
          |public class CustomTypeDeserializer {
          |
          |    public Result<CustomType> deserialize(DeserializationContext context) {
          |        return Result.ok(
          |                new CustomType("hello")
          |        );
          |    }
          |}
          |""".stripMargin
      )
      .intoUnitTest((processingEnvironment, element) => {
        ToolingProvider.setTooling(processingEnvironment)
        val customDeserializers = new CustomDeserializers()
        customDeserializers.registerCustomDeserializer(element)
      })
      .thenExpectThat()
      .compilationFails()
      .andThat()
      .compilerMessage()
      .ofKindError()
      .contains("Non static custom deserializers aren't supported yet")
      .executeTest()
  }

  test("testSuccessfulRegistrationInterface") {
    Cute
      .unitTest()
      .when()
      .passInElement()
      .fromSourceString[TypeElement](
        "me.bristermitten.mittenlib.annotations.integration.extension.CustomTypeDeserializer",
        """
          |import io.toolisticon.cute.PassIn;
          |import me.bristermitten.mittenlib.annotations.integration.extension.CustomType;
          |import me.bristermitten.mittenlib.config.DeserializationContext;
          |import me.bristermitten.mittenlib.config.extension.CustomDeserializer;
          |import me.bristermitten.mittenlib.config.extension.CustomDeserializerFor;
          |import me.bristermitten.mittenlib.util.Result;
          |
          |@CustomDeserializerFor(CustomType.class)
          |@PassIn
          |public class CustomTypeDeserializer implements CustomDeserializer<CustomType> {
          |    @Override
          |    public Result<CustomType> apply(DeserializationContext context) {
          |        return Result.ok(
          |                new CustomType("hello")
          |        );
          |    }
          |}
          |""".stripMargin
      )
      .intoUnitTest((processingEnvironment, element) => {
        ToolingProvider.setTooling(processingEnvironment)
        val customDeserializers = new CustomDeserializers()
        customDeserializers.registerCustomDeserializer(element)
      })
      .thenExpectThat()
      .compilationSucceeds()
      .executeTest()
  }
}
