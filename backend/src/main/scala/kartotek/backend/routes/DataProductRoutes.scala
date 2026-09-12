package kartotek.backend.routes

import cats.effect.IO
import cats.syntax.all.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.dsl.io.*
import org.slf4j.LoggerFactory
import kartotek.backend.models.DataProduct
import kartotek.backend.repository.DataProductRepository

/** HTTP surface for data products (the "cards" in the index). */
class DataProductRoutes(repo: DataProductRepository) {

  private val logger = LoggerFactory.getLogger(getClass)

  /** Payload for creating a product — the server owns `id` and `createdAt`. */
  private case class NewDataProduct(
      name: String,
      description: String,
      market: String,
      project: String,
      createdBy: String
  )

  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {

    case GET -> Root / "data-products" =>
      repo.all.flatMap(products => Ok(products.asJson))

    case GET -> Root / "data-products" / id =>
      repo.findById(id).flatMap {
        case Some(product) => Ok(product.asJson)
        case None          => NotFound(s"No data product with id $id")
      }

    case req @ POST -> Root / "data-products" =>
      req
        .attemptAs[NewDataProduct](jsonOf[IO, NewDataProduct])
        .foldF(
          failure => BadRequest(s"Invalid data product payload: ${failure.message}"),
          payload => {
            val product = DataProduct(
              name = payload.name,
              description = payload.description,
              market = payload.market,
              project = payload.project,
              createdBy = payload.createdBy
            )
            logger.info(s"Creating data product '${product.name}' (${product.id})")
            repo.insert(product).flatMap(saved => Created(saved.asJson))
          }
        )
  }
}
