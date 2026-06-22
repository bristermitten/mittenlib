package me.bristermitten.mittenlib.annotations.parser

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class CompilerMessagesTest extends AnyFunSuite with Matchers {

  test("testCustomDeserializersCompilerMessages") {
    val values = CustomDeserializersMessagesCompilerMessages.values()
    values.length should be > 0

    val valName =
      CustomDeserializersMessagesCompilerMessages.valueOf(values(0).name())
    valName should not be null
  }
}
