package me.bristermitten.mittenlib.annotations.config

import io.toolisticon.cute.Cute
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class NewtypeValidationTest extends AnyFunSuite with Matchers {

  test("testNormalClassWithNewtypeFails") {
    Cute
      .blackBoxTest()
      .`given`()
      .processor(classOf[ConfigProcessor])
      .andSourceFile(
        "InvalidClassNewtype",
        """
          |package me.bristermitten.mittenlib.tests;
          |
          |import me.bristermitten.mittenlib.config.Newtype;
          |
          |@Newtype
          |public class InvalidClassNewtype {
          |    private final String value;
          |    public InvalidClassNewtype(String value) {
          |        this.value = value;
          |    }
          |    public String value() { return value; }
          |}
          |""".stripMargin
      )
      .whenCompiled()
      .thenExpectThat()
      .compilationFails()
      .andThat()
      .compilerMessage()
      .ofKindError()
      .contains(
        "Newtype annotation only supports records and interfaces: me.bristermitten.mittenlib.tests.InvalidClassNewtype"
      )
      .executeTest()
  }

  test("testRecordWithZeroComponentsFails") {
    Cute
      .blackBoxTest()
      .`given`()
      .processor(classOf[ConfigProcessor])
      .andSourceFile(
        "EmptyRecordNewtype",
        """
          |package me.bristermitten.mittenlib.tests;
          |
          |import me.bristermitten.mittenlib.config.Newtype;
          |
          |@Newtype
          |public record EmptyRecordNewtype() {}
          |""".stripMargin
      )
      .whenCompiled()
      .thenExpectThat()
      .compilationFails()
      .andThat()
      .compilerMessage()
      .ofKindError()
      .contains(
        "Newtype record me.bristermitten.mittenlib.tests.EmptyRecordNewtype must have exactly one component"
      )
      .executeTest()
  }

  test("testRecordWithTwoComponentsFails") {
    Cute
      .blackBoxTest()
      .`given`()
      .processor(classOf[ConfigProcessor])
      .andSourceFile(
        "TwoComponentRecordNewtype",
        """
          |package me.bristermitten.mittenlib.tests;
          |
          |import me.bristermitten.mittenlib.config.Newtype;
          |
          |@Newtype
          |public record TwoComponentRecordNewtype(String first, int second) {}
          |""".stripMargin
      )
      .whenCompiled()
      .thenExpectThat()
      .compilationFails()
      .andThat()
      .compilerMessage()
      .ofKindError()
      .contains(
        "Newtype record me.bristermitten.mittenlib.tests.TwoComponentRecordNewtype must have exactly one component"
      )
      .executeTest()
  }

  test("testInterfaceWithZeroMethodsFails") {
    Cute
      .blackBoxTest()
      .`given`()
      .processor(classOf[ConfigProcessor])
      .andSourceFile(
        "EmptyInterfaceNewtype",
        """
          |package me.bristermitten.mittenlib.tests;
          |
          |import me.bristermitten.mittenlib.config.Newtype;
          |
          |@Newtype
          |public interface EmptyInterfaceNewtype {}
          |""".stripMargin
      )
      .whenCompiled()
      .thenExpectThat()
      .compilationFails()
      .andThat()
      .compilerMessage()
      .ofKindError()
      .contains(
        "Newtype interface me.bristermitten.mittenlib.tests.EmptyInterfaceNewtype must have exactly one abstract method"
      )
      .executeTest()
  }

  test("testInterfaceWithTwoMethodsFails") {
    Cute
      .blackBoxTest()
      .`given`()
      .processor(classOf[ConfigProcessor])
      .andSourceFile(
        "TwoMethodInterfaceNewtype",
        """
          |package me.bristermitten.mittenlib.tests;
          |
          |import me.bristermitten.mittenlib.config.Newtype;
          |
          |@Newtype
          |public interface TwoMethodInterfaceNewtype {
          |    String first();
          |    int second();
          |}
          |""".stripMargin
      )
      .whenCompiled()
      .thenExpectThat()
      .compilationFails()
      .andThat()
      .compilerMessage()
      .ofKindError()
      .contains(
        "Newtype interface me.bristermitten.mittenlib.tests.TwoMethodInterfaceNewtype must have exactly one abstract method"
      )
      .executeTest()
  }

  test("testGenericRecordNewtypeCompiles") {
    Cute
      .blackBoxTest()
      .`given`()
      .processor(classOf[ConfigProcessor])
      .andSourceFile(
        "GenericRecordNewtype",
        """
          |package me.bristermitten.mittenlib.tests;
          |
          |import me.bristermitten.mittenlib.config.Newtype;
          |
          |@Newtype
          |public record GenericRecordNewtype<T>(String value) {}
          |""".stripMargin
      )
      .whenCompiled()
      .thenExpectThat()
      .compilationSucceeds()
      .executeTest()
  }

  test("testGenericInterfaceNewtypeCompiles") {
    Cute
      .blackBoxTest()
      .`given`()
      .processor(classOf[ConfigProcessor])
      .andSourceFile(
        "GenericInterfaceNewtype",
        """
          |package me.bristermitten.mittenlib.tests;
          |
          |import me.bristermitten.mittenlib.config.Newtype;
          |
          |@Newtype
          |public interface GenericInterfaceNewtype<T> {
          |    T value();
          |}
          |""".stripMargin
      )
      .whenCompiled()
      .thenExpectThat()
      .compilationSucceeds()
      .executeTest()
  }

  test("testGenericNewtypeConfigCompiles") {
    Cute
      .blackBoxTest()
      .`given`()
      .processor(classOf[ConfigProcessor])
      .andSourceFile(
        "GenericNewtypeConfig",
        """
          |package me.bristermitten.mittenlib.tests;
          |
          |import me.bristermitten.mittenlib.config.Config;
          |import me.bristermitten.mittenlib.config.Newtype;
          |
          |@Config
          |public interface GenericNewtypeConfig {
          |    Id<String> id();
          |
          |    @Newtype
          |    interface Id<T> {
          |        T value();
          |    }
          |}
          |""".stripMargin
      )
      .whenCompiled()
      .thenExpectThat()
      .compilationSucceeds()
      .executeTest()
  }

  test("testGenericRecordNewtypeConfigCompiles") {
    Cute
      .blackBoxTest()
      .`given`()
      .processor(classOf[ConfigProcessor])
      .andSourceFile(
        "GenericRecordNewtypeConfig",
        """
          |package me.bristermitten.mittenlib.tests;
          |
          |import me.bristermitten.mittenlib.config.Config;
          |import me.bristermitten.mittenlib.config.Newtype;
          |
          |@Config
          |public interface GenericRecordNewtypeConfig {
          |    Id<String> id();
          |
          |    @Newtype
          |    record Id<T>(T value) {}
          |}
          |""".stripMargin
      )
      .whenCompiled()
      .thenExpectThat()
      .compilationSucceeds()
      .executeTest()
  }
}
