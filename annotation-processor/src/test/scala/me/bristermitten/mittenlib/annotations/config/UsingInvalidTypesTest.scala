package me.bristermitten.mittenlib.annotations.config

import com.google.testing.compile.Compiler.javac
import com.google.testing.compile.JavaFileObjects
import javax.tools.JavaFileObject
import me.bristermitten.mittenlib.annotations.exception.DTOReferenceException
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class UsingInvalidTypesTest extends AnyFunSuite with Matchers {

  test("generateConfigReferencingGeneratedType") {
    val compilation = javac().withProcessors(new ConfigProcessor())

    val source1: JavaFileObject =
      JavaFileObjects.forSourceString(
        "me.bristermitten.mittenlib.tests.OverriddenNameDTO",
        """
          |package me.bristermitten.mittenlib.tests;
          |import java.util.Map;
          |import me.bristermitten.mittenlib.config.*;
          |@Config
          |public class OverriddenNameDTO {
          |    public int clone;
          |}
          |""".stripMargin
      )
    val source2: JavaFileObject =
      JavaFileObjects.forSourceString(
        "me.bristermitten.mittenlib.tests.OtherDTO",
        """
          |package me.bristermitten.mittenlib.tests;
          |import java.util.Map;
          |import me.bristermitten.mittenlib.config.*;
          |@Config
          |public class OtherDTO {
          |    public me.bristermitten.mittenlib.tests.OverriddenName fail;
          |}
          |""".stripMargin
      )

    val exception = intercept[Exception] {
      compilation.compile(source1, source2)
    }
    exception.getCause shouldBe a[DTOReferenceException]
  }

  test("generateConfigReferencingNonExistentType") {
    val compilation = javac().withProcessors(new ConfigProcessor())

    val source1: JavaFileObject =
      JavaFileObjects.forSourceString(
        "me.bristermitten.mittenlib.tests.OverriddenNameDTO",
        """
          |package me.bristermitten.mittenlib.tests;
          |import java.util.Map;
          |import me.bristermitten.mittenlib.config.*;
          |@Config
          |public class OverriddenNameDTO {
          |    public DefinitelyAnInvalidNameIHope what;
          |}
          |""".stripMargin
      )

    val exception = intercept[Exception] {
      compilation.compile(source1)
    }
    exception.getCause shouldBe a[DTOReferenceException]
    exception.getCause.getMessage should include("DefinitelyAnInvalidNameIHope")
  }
}
