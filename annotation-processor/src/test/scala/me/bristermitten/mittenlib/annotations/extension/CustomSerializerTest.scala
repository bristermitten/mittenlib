package me.bristermitten.mittenlib.annotations.extension

import io.toolisticon.aptk.common.ToolingProvider
import io.toolisticon.cute.Cute
import javax.lang.model.element.TypeElement
import me.bristermitten.mittenlib.annotations.parser.CustomSerializers
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class CustomSerializerTest extends AnyFunSuite with Matchers {

  test("testSuccessfulRegistrationStaticMethod") {
    Cute
      .unitTest()
      .when()
      .passInElement()
      .fromSourceString[TypeElement](
        "me.bristermitten.mittenlib.annotations.extension.TestSerializer",
        """
          |import io.toolisticon.cute.PassIn;
          |import me.bristermitten.mittenlib.config.SerializationContext;
          |import me.bristermitten.mittenlib.config.extension.CustomSerializerFor;
          |import me.bristermitten.mittenlib.config.tree.DataTree;
          |
          |@CustomSerializerFor(String.class)
          |@PassIn
          |public class TestSerializer {
          |    public static DataTree serialize(String value, SerializationContext context) {
          |        return DataTree.string("serialized-" + value);
          |    }
          |}
          |""".stripMargin
      )
      .intoUnitTest((processingEnvironment, element) => {
        ToolingProvider.setTooling(processingEnvironment)
        val customSerializers = new CustomSerializers()
        customSerializers.registerCustomSerializer(element)
      })
      .thenExpectThat()
      .compilationSucceeds()
      .executeTest()
  }

  test("testSuccessfulRegistrationInterface") {
    Cute
      .unitTest()
      .when()
      .passInElement()
      .fromSourceString[TypeElement](
        "me.bristermitten.mittenlib.annotations.extension.TestSerializer",
        """
          |import io.toolisticon.cute.PassIn;
          |import me.bristermitten.mittenlib.config.SerializationContext;
          |import me.bristermitten.mittenlib.config.extension.CustomSerializer;
          |import me.bristermitten.mittenlib.config.extension.CustomSerializerFor;
          |import me.bristermitten.mittenlib.config.tree.DataTree;
          |
          |@CustomSerializerFor(String.class)
          |@PassIn
          |public class TestSerializer implements CustomSerializer<String> {
          |    @Override
          |    public DataTree apply(String value, SerializationContext context) {
          |        return DataTree.string("serialized-" + value);
          |    }
          |}
          |""".stripMargin
      )
      .intoUnitTest((processingEnvironment, element) => {
        ToolingProvider.setTooling(processingEnvironment)
        val customSerializers = new CustomSerializers()
        customSerializers.registerCustomSerializer(element)
      })
      .thenExpectThat()
      .compilationSucceeds()
      .executeTest()
  }

  test("testMissingAnnotationFails") {
    Cute
      .unitTest()
      .when()
      .passInElement()
      .fromSourceString[TypeElement](
        "me.bristermitten.mittenlib.annotations.extension.TestSerializer",
        """
          |import io.toolisticon.cute.PassIn;
          |import me.bristermitten.mittenlib.config.SerializationContext;
          |import me.bristermitten.mittenlib.config.tree.DataTree;
          |
          |@PassIn
          |public class TestSerializer {
          |    public static DataTree serialize(String value, SerializationContext context) {
          |        return DataTree.string("serialized-" + value);
          |    }
          |}
          |""".stripMargin
      )
      .intoUnitTest((processingEnvironment, element) => {
        ToolingProvider.setTooling(processingEnvironment)
        val customSerializers = new CustomSerializers()
        customSerializers.registerCustomSerializer(element)
      })
      .thenExpectThat()
      .compilationFails()
      .andThat()
      .compilerMessage()
      .ofKindError()
      .contains("CustomSerializer must be annotated with @CustomSerializerFor")
      .executeTest()
  }

  test("testMissingInterfaceOrMethodFails") {
    Cute
      .unitTest()
      .when()
      .passInElement()
      .fromSourceString[TypeElement](
        "me.bristermitten.mittenlib.annotations.extension.TestSerializer",
        """
          |import io.toolisticon.cute.PassIn;
          |import me.bristermitten.mittenlib.config.extension.CustomSerializerFor;
          |
          |@CustomSerializerFor(String.class)
          |@PassIn
          |public class TestSerializer {
          |    // Missing both serialize method and CustomSerializer interface
          |}
          |""".stripMargin
      )
      .intoUnitTest((processingEnvironment, element) => {
        ToolingProvider.setTooling(processingEnvironment)
        val customSerializers = new CustomSerializers()
        customSerializers.registerCustomSerializer(element)
      })
      .thenExpectThat()
      .compilationFails()
      .andThat()
      .compilerMessage()
      .ofKindError()
      .contains(
        "CustomSerializer must implement CustomSerializer or have a static method DataTree serialize(T, SerializationContext)"
      )
      .executeTest()
  }

  test("testWrongReturnTypeMethodFails") {
    Cute
      .unitTest()
      .when()
      .passInElement()
      .fromSourceString[TypeElement](
        "me.bristermitten.mittenlib.annotations.extension.TestSerializer",
        """
          |import io.toolisticon.cute.PassIn;
          |import me.bristermitten.mittenlib.config.SerializationContext;
          |import me.bristermitten.mittenlib.config.extension.CustomSerializerFor;
          |
          |@CustomSerializerFor(String.class)
          |@PassIn
          |public class TestSerializer {
          |    public static String serialize(String value, SerializationContext context) {
          |        return "serialized-" + value;
          |    }
          |}
          |""".stripMargin
      )
      .intoUnitTest((processingEnvironment, element) => {
        ToolingProvider.setTooling(processingEnvironment)
        val customSerializers = new CustomSerializers()
        customSerializers.registerCustomSerializer(element)
      })
      .thenExpectThat()
      .compilationFails()
      .andThat()
      .compilerMessage()
      .ofKindError()
      .contains("Custom serializer method must return DataTree")
      .executeTest()
  }

  test("testNonStaticMethodWithoutInterfaceFails") {
    Cute
      .unitTest()
      .when()
      .passInElement()
      .fromSourceString[TypeElement](
        "me.bristermitten.mittenlib.annotations.extension.TestSerializer",
        """
          |import io.toolisticon.cute.PassIn;
          |import me.bristermitten.mittenlib.config.SerializationContext;
          |import me.bristermitten.mittenlib.config.extension.CustomSerializerFor;
          |import me.bristermitten.mittenlib.config.tree.DataTree;
          |
          |@CustomSerializerFor(String.class)
          |@PassIn
          |public class TestSerializer {
          |    // Method is not static, and class doesn't implement CustomSerializer
          |    public DataTree serialize(String value, SerializationContext context) {
          |        return DataTree.string("serialized-" + value);
          |    }
          |}
          |""".stripMargin
      )
      .intoUnitTest((processingEnvironment, element) => {
        ToolingProvider.setTooling(processingEnvironment)
        val customSerializers = new CustomSerializers()
        customSerializers.registerCustomSerializer(element)
      })
      .thenExpectThat()
      .compilationFails()
      .andThat()
      .compilerMessage()
      .ofKindError()
      .contains("Non static custom serializers must implement CustomSerializer")
      .executeTest()
  }
}
