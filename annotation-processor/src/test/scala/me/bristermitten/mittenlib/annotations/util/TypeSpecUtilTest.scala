package me.bristermitten.mittenlib.annotations.util

import com.palantir.javapoet.MethodSpec
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class TypeSpecUtilTest extends AnyFunSuite with Matchers {

  test("testMethodAddAnnotationAddsNonRepeatableOnce") {
    val builder = MethodSpec.methodBuilder("test")
    TypeSpecUtil.methodAddAnnotation(
      builder,
      classOf[IntegrationTests.NonRepeatableAnnotation]
    )
    builder.build().annotations().size() shouldBe 1

    TypeSpecUtil.methodAddAnnotation(
      builder,
      classOf[IntegrationTests.NonRepeatableAnnotation]
    )
    builder.build().annotations().size() shouldBe 1 // Should not add again
  }

  test("testMethodAddAnnotationAddsRepeatableMultipleTimes") {
    val builder = MethodSpec.methodBuilder("test")
    TypeSpecUtil.methodAddAnnotation(
      builder,
      classOf[IntegrationTests.RepeatableAnnotation]
    )
    builder.build().annotations().size() shouldBe 1

    TypeSpecUtil.methodAddAnnotation(
      builder,
      classOf[IntegrationTests.RepeatableAnnotation]
    )
    builder.build().annotations().size() shouldBe 2 // Should add again
  }
}
