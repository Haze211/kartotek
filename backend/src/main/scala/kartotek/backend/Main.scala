package kartotek.backend

import cats.effect.{ExitCode, IO, IOApp, Resource}
import cats.syntax.semigroupk.*
import com.comcast.ip4s.{Host, Port}
import mongo4cats.client.MongoClient
import org.http4s.HttpApp
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.{CORS, Logger as RequestLogger}
import org.slf4j.LoggerFactory
import kartotek.backend.config.AppConfig
import kartotek.backend.repository.{ChangeLogRepository, DataProductRepository}
import kartotek.backend.routes.{ChangeLogRoutes, DataProductRoutes}

object Main extends IOApp {

  private val logger = LoggerFactory.getLogger(getClass)

  private def httpApp(client: MongoClient[IO], config: AppConfig): HttpApp[IO] = {
    val productRepo   = new DataProductRepository(client, config.mongoDbName)
    val changeLogRepo = new ChangeLogRepository(client, config.mongoDbName)

    val routes =
      new DataProductRoutes(productRepo).routes <+>
        new ChangeLogRoutes(changeLogRepo, productRepo).routes

    // CORS so the Vite dev server on another port can call the API.
    RequestLogger.httpApp(logHeaders = false, logBody = false)(
      CORS.policy.withAllowOriginAll(routes).orNotFound
    )
  }

  private def server(config: AppConfig): Resource[IO, Unit] =
    for {
      client <- MongoClient.fromConnectionString[IO](config.mongoUri)
      _ <- EmberServerBuilder
        .default[IO]
        .withHost(Host.fromString("0.0.0.0").get)
        .withPort(Port.fromInt(config.httpPort).get)
        .withHttpApp(httpApp(client, config))
        .build
    } yield ()

  override def run(args: List[String]): IO[ExitCode] = {
    val config = AppConfig.load()
    IO(logger.info(s"Starting Kartotek on port ${config.httpPort}, mongo db '${config.mongoDbName}'")) *>
      server(config).useForever.as(ExitCode.Success)
  }
}
