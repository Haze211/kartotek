package kartotek.frontend

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import kartotek.frontend.models.{ChangeLog, DataProduct}

/** Kartotek — a card index of data product baselines.
  *
  * The card-stack sidebar and expand-into-log interaction are carried over
  * from the original prototype; the data behind them now comes from the API
  * instead of hardcoded mocks.
  */
object App {

  private val products: Var[List[DataProduct]]     = Var(Nil)
  private val selectedProduct: Var[Option[DataProduct]] = Var(None)
  private val changeLogs: Var[List[ChangeLog]]     = Var(Nil)
  private val errorMessage: Var[Option[String]]    = Var(None)

  private val showAddProduct: Var[Boolean] = Var(false)
  private val showAddLog: Var[Boolean]     = Var(false)

  def main(args: Array[String]): Unit = {
    renderOnDomContentLoaded(dom.document.body, appElement)
  }

  // --- data loading -------------------------------------------------------

  private def loadProducts(): Unit =
    ApiClient.dataProducts().onComplete {
      case Success(list) =>
        products.set(list)
        errorMessage.set(None)
      case Failure(err) =>
        errorMessage.set(Some(s"Could not load data products: ${err.getMessage}"))
    }

  private def selectProduct(product: DataProduct): Unit =
    selectedProduct.now() match {
      case Some(current) if current.id == product.id =>
        selectedProduct.set(None)
        changeLogs.set(Nil)
      case _ =>
        selectedProduct.set(Some(product))
        changeLogs.set(Nil)
        ApiClient.changeLog(product.id).onComplete {
          case Success(logs) =>
            changeLogs.set(logs)
            errorMessage.set(None)
          case Failure(err) =>
            errorMessage.set(Some(s"Could not load change log: ${err.getMessage}"))
        }
    }

  // --- layout -------------------------------------------------------------

  private def appElement: HtmlElement =
    div(
      cls := "app-container",
      onMountCallback(_ => loadProducts()),
      headerBar,
      child.maybe <-- errorMessage.signal.map(_.map(msg => div(cls := "error-banner", msg))),
      div(
        cls := "content",
        sidebar,
        mainPanel
      ),
      child.maybe <-- showAddProduct.signal.map(if (_) Some(addProductModal) else None),
      child.maybe <-- showAddLog.signal.map(if (_) Some(addLogPanel) else None)
    )

  private def headerBar: HtmlElement =
    header(
      cls := "header-bar",
      h4("Kartotek"),
      div(
        cls := "header-icons",
        span("🔍"),
        span("⚙️")
      )
    )

  private def sidebar: HtmlElement =
    div(
      cls := "sidebar",
      button(
        cls := "add-button",
        "Add Data Product",
        onClick.mapTo(true) --> showAddProduct
      ),
      div(
        cls := "card-stack",
        children <-- products.signal.combineWith(selectedProduct.signal).map { case (list, selected) =>
          list.zipWithIndex.map { case (product, index) =>
            renderCard(product, selected.exists(_.id == product.id), index)
          }
        }
      ),
      child.maybe <-- products.signal.map { list =>
        Option.when(list.isEmpty)(p(cls := "empty-hint", "No data products yet."))
      }
    )

  /** A card in the index: stacked with a staggered offset, tab peeking out. */
  private def renderCard(product: DataProduct, isSelected: Boolean, index: Int): HtmlElement =
    div(
      cls := "card",
      cls.toggle("selected") := isSelected,
      styleAttr := s"top: ${index * 50}px",
      div(cls := "card-tab", product.market),
      div(cls := "card-title", product.name),
      onClick --> { _ => selectProduct(product) }
    )

  private def mainPanel: HtmlElement =
    div(
      cls := "main-content",
      child <-- selectedProduct.signal.map {
        case None =>
          div(
            h2("Welcome to Kartotek"),
            p("Select a data product on the left to see its change log.")
          )
        case Some(product) =>
          div(
            cls := "main-card expanded",
            div(
              cls := "card-header",
              h2(product.name),
              span(
                cls := "close-icon",
                "✖",
                onClick --> { _ => selectedProduct.set(None) }
              )
            ),
            div(
              cls := "baseline-meta",
              p(b("Market: "), product.market),
              p(b("Project: "), product.project),
              p(b("Created by: "), product.createdBy),
              p(product.description)
            ),
            button(
              cls := "add-button",
              "Add Log Entry",
              onClick.mapTo(true) --> showAddLog
            ),
            div(
              cls := "change-log-list",
              children <-- changeLogs.signal.map(_.map(renderLogEntry))
            )
          )
      }
    )

  private def renderLogEntry(log: ChangeLog): HtmlElement =
    div(
      cls := "change-log-entry",
      div(
        cls := "change-log-header",
        span(cls := "log-date", log.requestedOn),
        span(cls := "dotted-line")
      ),
      div(
        cls := "log-description",
        p(b("Change: "), log.changeDescription),
        p(b("Requested by: "), log.requestedBy),
        p(log.detailedDescription)
      )
    )

  // --- forms --------------------------------------------------------------

  private def field(label: String, target: Var[String]): HtmlElement =
    div(
      cls := "form-field",
      span(cls := "form-label", label),
      input(
        typ := "text",
        value <-- target.signal,
        onInput.mapToValue --> target
      )
    )

  private def addProductModal: HtmlElement = {
    val name        = Var("")
    val description = Var("")
    val market      = Var("")
    val project     = Var("")
    val createdBy   = Var("")

    def submit(): Unit =
      ApiClient
        .addDataProduct(name.now(), description.now(), market.now(), project.now(), createdBy.now())
        .onComplete {
          case Success(_) =>
            showAddProduct.set(false)
            loadProducts()
          case Failure(err) =>
            errorMessage.set(Some(s"Could not add data product: ${err.getMessage}"))
        }

    div(
      cls := "modal-backdrop",
      div(
        cls := "modal-content",
        h2("Add New Data Product"),
        field("Name", name),
        field("Description", description),
        field("Market", market),
        field("Project", project),
        field("Created by", createdBy),
        button(cls := "add-button", "Save", onClick --> { _ => submit() }),
        button(cls := "close-button", "Cancel", onClick.mapTo(false) --> showAddProduct)
      )
    )
  }

  private def addLogPanel: HtmlElement = {
    val changeDescription   = Var("")
    val requestedBy         = Var("")
    val detailedDescription = Var("")

    def submit(): Unit =
      selectedProduct.now().foreach { product =>
        ApiClient
          .addChangeLog(product.id, changeDescription.now(), requestedBy.now(), detailedDescription.now())
          .onComplete {
            case Success(_) =>
              showAddLog.set(false)
              selectedProduct.set(None)
              selectProduct(product) // reload the log for this card
            case Failure(err) =>
              errorMessage.set(Some(s"Could not add log entry: ${err.getMessage}"))
          }
      }

    div(
      cls := "slide-panel",
      div(
        cls := "slide-content",
        h2("Add Log Entry"),
        field("Change", changeDescription),
        field("Requested by", requestedBy),
        field("Details", detailedDescription),
        button(cls := "add-button", "Save", onClick --> { _ => submit() }),
        button(cls := "close-button", "Cancel", onClick.mapTo(false) --> showAddLog)
      )
    )
  }
}
