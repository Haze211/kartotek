package kartotek.backend.repository

import cats.effect.IO
import io.circe.generic.auto.*
import io.circe.syntax.*
import mongo4cats.circe.*
import mongo4cats.client.MongoClient
import mongo4cats.bson.Document
import mongo4cats.operations.Filter
import kartotek.backend.models.DataProduct

/** Talks to the `data_products` collection. Kept as a thin repository so
  * routes never touch the Mongo client directly.
  */
class DataProductRepository(client: MongoClient[IO], dbName: String) {

  private def collection = client.getDatabase(dbName).flatMap(_.getCollectionWithCodec[DataProduct]("data_products"))

  def all: IO[List[DataProduct]] =
    collection.flatMap(_.find.all.map(_.toList))

  def findById(id: String): IO[Option[DataProduct]] =
    collection.flatMap(_.find.filter(Filter.eq("id", id)).first)

  def insert(product: DataProduct): IO[DataProduct] =
    collection.flatMap(_.insertOne(product)).as(product)
}
