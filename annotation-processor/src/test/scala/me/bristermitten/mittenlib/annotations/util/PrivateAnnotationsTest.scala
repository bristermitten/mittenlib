package me.bristermitten.mittenlib.annotations.util

import me.bristermitten.mittenlib.config.names.ConfigName
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class PrivateAnnotationsTest extends AnyFunSuite with Matchers {

  test("testIsPrivate") {
    PrivateAnnotations.isPrivate(classOf[ConfigName].getName) shouldBe true
    PrivateAnnotations.isPrivate("some.other.Annotation") shouldBe false
  }
}
