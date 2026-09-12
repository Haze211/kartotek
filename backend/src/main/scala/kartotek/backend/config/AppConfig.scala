package kartotek.backend.config

case class AppConfig(mongoUri: String, mongoDbName: String, httpPort: Int)

object AppConfig {

  def load(): AppConfig = AppConfig(
    mongoUri = sys.env.getOrElse("KARTOTEK_MONGO_URI", "mongodb://localhost:27017"),
    mongoDbName = sys.env.getOrElse("KARTOTEK_MONGO_DB", "kartotek"),
    httpPort = sys.env.get("KARTOTEK_HTTP_PORT").flatMap(_.toIntOption).getOrElse(8080)
  )
}
