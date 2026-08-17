class MainSpec extends munit.FunSuite:

  test("greet returns a greeting for the given name") {
    assertEquals(greet("Scala"), "Hello, Scala!")
  }
