package me.bristermitten.mittenlib.annotations.exception

class ConfigProcessingExceptionTest extends munit.FunSuite:

  test("constructor") {
    val cause = new RuntimeException("root cause")
    val exception = new ConfigProcessingException("error occurred", cause)

    assertEquals(exception.getMessage, "error occurred")
    assert(exception.getCause eq cause)
  }
