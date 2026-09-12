package kartotek.backend.routes

import cats.effect.IO
import cats.syntax.all.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.dsl.io.*
import org.slf4j.LoggerFactory
import java.time.Instant
import kartotek.backend.models.ChangeLog
import kartotek.backend.repository.{ChangeLogRepository, DataProductRepository}

/** HTTP surface for the change log hanging off each data product —
  * who asked for what, when, and why.
  */
class ChangeLogRoutes(changeLogs: ChangeLogRepository, products: DataProductRepository) {

  private val logger = LoggerFactory.getLogger(getClass)

  /** Payload for appending a log entry — `requestedAt` defaults to now. */
  private case class NewChangeLog(
      changeDescription: String,
      requestedBy: String,
      detailedDescription: String,
      requestedAt: Option[Instant]
  )

  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {

    case GET -> Root / "data-products" / productId / "change-log" =>
      changeLogs.forProduct(productId).flatMap(entries => Ok(entries.asJson))

    case req @ POST -> Root / "data-products" / productId / "change-log" =>
      products.findById(productId).flatMap {
        case None =>
          NotFound(s"No data product with id $productId")
        case Some(_) =>
          req
            .attemptAs[NewChangeLog](jsonOf[IO, NewChangeLog])
            .foldF(
              failure => BadRequest(s"Invalid change log payload: ${failure.message}"),
              payload => {
                val entry = ChangeLog(
                  productId = productId,
                  changeDescription = payload.changeDescription,
                  requestedAt = payload.requestedAt.getOrElse(Instant.now()),
                  requestedBy = payload.requestedBy,
                  detailedDescription = payload.detailedDescription
                )
                logger.info(s"Appending change log entry for product $productId")
                changeLogs.insert(entry).flatMap(saved => Created(saved.asJson))
              }
            )
      }
  }
}
