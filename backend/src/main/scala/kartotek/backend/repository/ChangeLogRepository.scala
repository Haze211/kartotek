package kartotek.backend.repository

import cats.effect.IO
import io.circe.generic.auto.*
import mongo4cats.circe.*
import mongo4cats.client.MongoClient
import mongo4cats.operations.Filter
import kartotek.backend.models.ChangeLog

/** Talks to the `change_log` collection, keyed by `productId`. */
class ChangeLogRepository(client: MongoClient[IO], dbName: String) {

  private def collection = client.getDatabase(dbName).flatMap(_.getCollectionWithCodec[ChangeLog]("change_log"))

  def forProduct(productId: String): IO[List[ChangeLog]] =
    collection.flatMap(_.find.filter(Filter.eq("productId", productId)).all.map(_.toList))

  def insert(entry: ChangeLog): IO[ChangeLog] =
    collection.flatMap(_.insertOne(entry)).as(entry)
}
