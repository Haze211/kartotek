package kartotek.frontend

import org.scalajs.dom
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import kartotek.frontend.models.{ChangeLog, DataProduct}

/** Thin wrapper over the backend API.
  *
  * JSON is read via `js.Dynamic` to keep the frontend dependency-free. If
  * the payloads grow, swap this for circe on a shared cross-built module.
  */
object ApiClient {

  private val baseUrl: String =
    Option(dom.window.localStorage.getItem("kartotek.apiUrl")).filter(_.nonEmpty).getOrElse("http://localhost:8080")

  private def getJson(path: String): Future[js.Dynamic] =
    dom
      .fetch(s"$baseUrl$path")
      .toFuture
      .flatMap { response =>
        if (response.ok) response.text().toFuture
        else Future.failed(new RuntimeException(s"GET $path failed: ${response.status}"))
      }
      .map(js.JSON.parse(_))

  private def postJson(path: String, payload: js.Any): Future[js.Dynamic] = {
    val payloadJson = js.JSON.stringify(payload)
    val init = new dom.RequestInit {
      method = dom.HttpMethod.POST
      headers = js.Dictionary("Content-Type" -> "application/json")
      body = payloadJson
    }
    dom
      .fetch(s"$baseUrl$path", init)
      .toFuture
      .flatMap { response =>
        if (response.ok) response.text().toFuture
        else Future.failed(new RuntimeException(s"POST $path failed: ${response.status}"))
      }
      .map(js.JSON.parse(_))
  }

  private def str(d: js.Dynamic, field: String): String =
    Option(d.selectDynamic(field)).map(_.toString).filterNot(_ == "undefined").getOrElse("")

  private def toDataProduct(d: js.Dynamic): DataProduct = DataProduct(
    id = str(d, "id"),
    name = str(d, "name"),
    description = str(d, "description"),
    market = str(d, "market"),
    project = str(d, "project"),
    createdBy = str(d, "createdBy")
  )

  private def toChangeLog(d: js.Dynamic): ChangeLog = ChangeLog(
    productId = str(d, "productId"),
    changeDescription = str(d, "changeDescription"),
    requestedAt = str(d, "requestedAt"),
    requestedBy = str(d, "requestedBy"),
    detailedDescription = str(d, "detailedDescription")
  )

  def dataProducts(): Future[List[DataProduct]] =
    getJson("/data-products").map(_.asInstanceOf[js.Array[js.Dynamic]].toList.map(toDataProduct))

  def changeLog(productId: String): Future[List[ChangeLog]] =
    getJson(s"/data-products/$productId/change-log")
      .map(_.asInstanceOf[js.Array[js.Dynamic]].toList.map(toChangeLog))

  def addDataProduct(
      name: String,
      description: String,
      market: String,
      project: String,
      createdBy: String
  ): Future[DataProduct] =
    postJson(
      "/data-products",
      js.Dynamic.literal(
        name = name,
        description = description,
        market = market,
        project = project,
        createdBy = createdBy
      )
    ).map(toDataProduct)

  def addChangeLog(
      productId: String,
      changeDescription: String,
      requestedBy: String,
      detailedDescription: String
  ): Future[ChangeLog] =
    postJson(
      s"/data-products/$productId/change-log",
      js.Dynamic.literal(
        changeDescription = changeDescription,
        requestedBy = requestedBy,
        detailedDescription = detailedDescription
      )
    ).map(toChangeLog)
}
