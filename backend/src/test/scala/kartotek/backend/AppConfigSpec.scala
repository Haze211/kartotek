package kartotek.backend

import kartotek.backend.config.AppConfig

class AppConfigSpec extends munit.FunSuite {

  test("config falls back to local defaults when no env vars are set") {
    val config = AppConfig.load()
    assert(config.mongoUri.startsWith("mongodb://"))
    assert(config.mongoDbName.nonEmpty)
    assert(config.httpPort > 0)
  }
}
