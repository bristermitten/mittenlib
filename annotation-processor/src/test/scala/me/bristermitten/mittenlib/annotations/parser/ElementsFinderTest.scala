package me.bristermitten.mittenlib.annotations.parser

import io.toolisticon.aptk.common.ToolingProvider
import io.toolisticon.cute.Cute
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import me.bristermitten.mittenlib.annotations.util.ElementsFinder
import org.assertj.core.api.Assertions.assertThat
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import scala.jdk.javaapi.CollectionConverters

class ElementsFinderTest extends AnyFunSuite with Matchers {

  test("testGetApplicableVariableElements") {
    Cute
      .unitTest()
      .when()
      .passInElement()
      .fromSourceString[TypeElement](
        "me.bristermitten.mittenlib.tests.TestClass",
        """
          |package me.bristermitten.mittenlib.tests;
          |import io.toolisticon.cute.PassIn;
          |
          |@PassIn
          |public class TestClass {
          |    public int publicField;
          |    protected int protectedField;
          |    int packagePrivateField;
          |    private int privateField;
          |    public static int staticField;
          |    public transient int transientField;
          |}
          |""".stripMargin
      )
      .intoUnitTest((processingEnvironment, element) => {
        ToolingProvider.setTooling(processingEnvironment)
        val finder = new ElementsFinder(processingEnvironment.getElementUtils)

        val fields = CollectionConverters.asJava(
          finder.getApplicableVariableElements(element)
        )
        assertThat(fields)
          .extracting((f: VariableElement) => f.getSimpleName.toString)
          .containsExactlyInAnyOrder(
            "publicField",
            "protectedField",
            "packagePrivateField",
            "privateField"
          )
        assertThat(fields)
          .extracting((f: VariableElement) => f.getSimpleName.toString)
          .doesNotContain("staticField", "transientField")
      })
      .thenExpectThat()
      .compilationSucceeds()
      .executeTest()
  }

  test("testGetPropertyMethods") {
    Cute
      .unitTest()
      .when()
      .passInElement()
      .fromSourceString[TypeElement](
        "me.bristermitten.mittenlib.tests.TestClass",
        """
          |package me.bristermitten.mittenlib.tests;
          |import io.toolisticon.cute.PassIn;
          |
          |@PassIn
          |public class TestClass {
          |    public int getPublicMethod() {
          |        return 0;
          |    }
          |
          |    private int privateMethod() {
          |        return 0;
          |    }
          |
          |    public static int staticMethod() {
          |        return 0;
          |    }
          |
          |    public void voidMethod() {}
          |
          |    public int methodWithParams(int x) {
          |        return x;
          |    }
          |}
          |""".stripMargin
      )
      .intoUnitTest((processingEnvironment, element) => {
        ToolingProvider.setTooling(processingEnvironment)
        val finder = new ElementsFinder(processingEnvironment.getElementUtils)

        val methods =
          CollectionConverters.asJava(finder.getPropertyMethods(element))
        assertThat(methods)
          .extracting((m: ExecutableElement) => m.getSimpleName.toString)
          .contains(
            "getPublicMethod",
            "privateMethod",
            "staticMethod",
            "voidMethod"
          )
        assertThat(methods)
          .extracting((m: ExecutableElement) => m.getSimpleName.toString)
          .doesNotContain("methodWithParams")
      })
      .thenExpectThat()
      .compilationSucceeds()
      .executeTest()
  }
}
