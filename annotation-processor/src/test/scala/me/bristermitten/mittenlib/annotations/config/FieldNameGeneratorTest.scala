package me.bristermitten.mittenlib.annotations.config

import com.google.testing.compile.Compilation
import com.google.testing.compile.Compiler.javac
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaFileObjectSubject.assertThat as assertJavaFileObject
import java.util.regex.Pattern
import javax.tools.JavaFileObject
import me.bristermitten.mittenlib.config.names.NamingPatterns
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class FieldNameGeneratorTest extends AnyFunSuite with Matchers {

  private def compileField(source: String): JavaFileObject = {
    compileField(source, null)
  }

  private def compileField(
      source: String,
      pattern: NamingPatterns
  ): JavaFileObject = {
    val patternString =
      if (pattern == null) ""
      else s"@NamingPattern(NamingPatterns.${pattern.name()})"
    val compilation: Compilation = javac()
      .withProcessors(new ConfigProcessor())
      .compile(
        JavaFileObjects.forSourceString(
          "me.bristermitten.mittenlib.tests.FieldClassNameGeneratorTestDTO",
          s"""
           |package me.bristermitten.mittenlib.tests;
           |import java.util.Map;
           |
           |import me.bristermitten.mittenlib.config.*;
           |import me.bristermitten.mittenlib.config.names.*;
           |@Config
           |$patternString
           |public class FieldClassNameGeneratorTestDTO {
           |$source
           |}
           |""".stripMargin
        )
      )

    compilation
      .generatedSourceFile(
        "me.bristermitten.mittenlib.tests.FieldClassNameGeneratorTestDeserializer"
      )
      .orElseThrow()
  }

  private def assertConfigKeyUsed(source: JavaFileObject, key: String): Unit = {
    assertJavaFileObject(source)
      .contentsAsUtf8String()
      .containsMatch(
        Pattern.compile(s"\\$$data\\.(get|getOrDefault)\\(\"$key\"")
      )
  }

  test("assertThat_unannotatedFieldName_isIdentity") {
    val source = compileField("int hello;")
    assertConfigKeyUsed(source, "hello")
  }

  test("assertThat_annotatedFieldName_hasHigherPriority_withConfigName") {
    val source = compileField("""
        |@ConfigName("field-name")
        |int hello;
        |""".stripMargin)
    assertConfigKeyUsed(source, "field-name")
  }

  test("assertThat_annotatedConfigName_hasHigherPriority_thanNamingPattern") {
    val source = compileField(
      """
        |@ConfigName("field-name")
        |int hello;
        |""".stripMargin,
      NamingPatterns.LOWER_SNAKE_CASE
    )
    assertConfigKeyUsed(source, "field-name")
  }

  test(
    "assertThat_field_with_NamingPattern_hasHigherPriority_than_class_with_NamingPattern"
  ) {
    val source = compileField(
      """
        |@NamingPattern(NamingPatterns.UPPER_CAMEL_CASE)
        |int fieldName;
        |""".stripMargin,
      NamingPatterns.LOWER_SNAKE_CASE
    )
    assertConfigKeyUsed(source, "FieldName")
  }

  test("assertThat_unannotatedFieldName_usesClass_withNamingPattern") {
    val source = compileField(
      """
        |int fieldName;
        |""".stripMargin,
      NamingPatterns.LOWER_KEBAB_CASE
    )
    assertConfigKeyUsed(source, "field-name")
  }
}
