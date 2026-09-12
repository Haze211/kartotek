package kartotek.backend.config

case class AppConfig(mongoUri: String, mongoDbName: String, httpPort: Int)

object AppConfig {

  /** Matches the dev credentials in docker-compose.yml. `authSource=admin` is
    * required: the root user lives in the `admin` database, not in `kartotek`.
    */
  private val LocalDevMongoUri = "mongodb://admin:admin@localhost:27017/?authSource=admin"

  def load(): AppConfig = AppConfig(
    mongoUri = sys.env.getOrElse("KARTOTEK_MONGO_URI", LocalDevMongoUri),
    mongoDbName = sys.env.getOrElse("KARTOTEK_MONGO_DB", "kartotek"),
    httpPort = sys.env.get("KARTOTEK_HTTP_PORT").flatMap(_.toIntOption).getOrElse(8080)
  )
}
