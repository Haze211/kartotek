package kartotek.frontend.models

/** View models for the UI. These mirror the backend's `DataProduct` and
  * `ChangeLog` case classes.
  *
  * Next step worth taking: move these into a cross-compiled `shared` module
  * so the JVM and JS sides can't drift apart.
  */
case class DataProduct(
    id: String,
    name: String,
    description: String,
    market: String,
    project: String,
    createdBy: String
)

case class ChangeLog(
    productId: String,
    changeDescription: String,
    requestedAt: String,
    requestedBy: String,
    detailedDescription: String
) {
  /** "2025-04-26T15:00:00Z" -> "2025-04-26" */
  def requestedOn: String = requestedAt.take(10)
}
