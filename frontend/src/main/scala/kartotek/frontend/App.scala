package kartotek.frontend

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import kartotek.frontend.models.{ChangeLog, DataProduct}

/** Kartotek — a card index of data product baselines.
  *
  * Visual design ported from the "Kartotek Modern" design canvas: a light
  * OKLCH palette, Instrument Sans + JetBrains Mono, and a card stack where
  * hovering a card opens it in place while the cards in front of it slide
  * down by a fixed gap to make room. See styles.css for the class names
  * this markup depends on.
  */
object App {

  // Layout constants — must match the numbers baked into styles.css
  // (.slot height, .card height) since the stack's geometry is computed here
  // and only the per-card position is expressed as inline style.
  private val TabH    = 54  // height of a card's visible strip in the stack
  private val CardH   = 204 // height of a fully open card
  private val OpenGap = 158 // how far cards in front slide down to clear the open one

  // Cycled by card index — same four accent tints as the design canvas.
  private val Tints = List(
    "oklch(0.55 0.09 30)",
    "oklch(0.55 0.09 150)",
    "oklch(0.55 0.09 250)",
    "oklch(0.55 0.09 85)"
  )

  private val products: Var[List[DataProduct]]          = Var(Nil)
  private val selectedProduct: Var[Option[DataProduct]] = Var(None)
  private val changeLogs: Var[List[ChangeLog]]          = Var(Nil)
  private val errorMessage: Var[Option[String]]         = Var(None)

  // Which card is currently "open" from hover — separate from selection, so
  // hovering a different card previews it without losing your actual pick.
  private val hoveredIndex: Var[Option[Int]] = Var(None)

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

  /** "PRD-001" etc. — presentational only, derived from list position. Not
    * a stable identifier: it shifts if products are reordered or deleted.
    * A real product code would need to come from the backend.
    */
  private def productCode(index: Int): String = f"PRD-${index + 1}%03d"

  // --- layout -------------------------------------------------------------

  private def appElement: HtmlElement =
    div(
      cls := "app",
      onMountCallback(_ => loadProducts()),
      headerBar,
      child.maybe <-- errorMessage.signal.map(_.map(msg => div(cls := "error-banner", msg))),
      div(
        cls := "layout",
        sidebar,
        mainPanel
      ),
      child.maybe <-- showAddProduct.signal.map(if (_) Some(addProductModal) else None),
      child.maybe <-- showAddLog.signal.map(if (_) Some(addLogPanel) else None)
    )

  private def headerBar: HtmlElement =
    header(
      div(
        cls := "brand",
        span(cls := "brand-name", "Kartotek"),
        span(cls := "brand-sub mono", "data product index")
      ),
      div(
        cls := "search",
        span(cls := "search-dot"),
        // Not wired up yet — decorative, matching the design canvas.
        input(typ := "text", placeholder := "Search products, markets, changes"),
        kbd(cls := "mono", "⌘K")
      ),
      div(
        cls := "header-right",
        span(
          cls := "header-count mono",
          child.text <-- products.signal.map(list => s"${list.size} products")
        ),
        span(cls := "header-settings", "Settings"),
        // Placeholder until there's a real user/auth concept.
        span(cls := "avatar", "YK")
      )
    )

  private def sidebar: HtmlElement =
    aside(
      div(
        cls := "box-label-row",
        span(cls := "box-label mono", "The box"),
        button(cls := "btn-new", "+ New", onClick.mapTo(true) --> showAddProduct)
      ),
      div(
        cls := "box",
        div(
          cls := "stack",
          styleAttr <-- products.signal.map(list => s"height:${stackHeight(list.size)}px"),
          children <-- products.signal
            .combineWith(selectedProduct.signal, hoveredIndex.signal, changeLogs.signal)
            .map { case (list, selected, hovered, logs) =>
              val selIdx = selected.flatMap(s => list.indexWhere(_.id == s.id) match {
                case -1 => None
                case i  => Some(i)
              })
              val openIdx = hovered.orElse(selIdx)
              list.zipWithIndex.map { case (product, i) =>
                renderCard(product, i, list.size, openIdx, selIdx, logs)
              }
            }
        )
      ),
      p(cls := "stack-hint mono", "hover to lift · click to open")
    )

  private def stackHeight(n: Int): Int =
    if (n == 0) CardH else CardH + (n - 1) * TabH + OpenGap

  /** One card in the box. `openIdx` is whichever card is currently showing
    * its full body — from hover if hovering, otherwise the selected card.
    * Cards positioned in front of it (lower index) slide down by a fixed
    * gap to clear room; nothing about this is per-card computed distance,
    * it's the same OpenGap for all of them, which is what keeps their
    * relative stacking order intact while they move as one group.
    */
  private def renderCard(
      product: DataProduct,
      index: Int,
      total: Int,
      openIdx: Option[Int],
      selIdx: Option[Int],
      selectedLogs: List[ChangeLog]
  ): HtmlElement = {
    val top        = (total - 1 - index) * TabH
    val z          = total - index
    val isOpen     = openIdx.contains(index)
    val isSelected = selIdx.contains(index)
    val shiftDown  = openIdx.exists(index < _)
    val transform  = if (shiftDown) s"translateY(${OpenGap}px)" else "none"
    val tint       = Tints(index % Tints.length)

    // Log counts per card aren't available yet — the products list doesn't
    // carry a summary, and fetching each product's log just to render the
    // stack isn't worth an N+1 request. Show the real count only for the
    // card whose log we've actually loaded (the selected one); a dash
    // otherwise. Fixing this properly means adding a lightweight
    // changeLogCount/lastChangeAt to the GET /data-products response.
    val (entriesLabel, lastLabel) =
      if (isSelected) {
        val last = selectedLogs.lastOption.map(_.requestedOn).getOrElse("—")
        (s"${selectedLogs.size}${if (selectedLogs.size == 1) " ENTRY" else " ENTRIES"}", s"LAST $last")
      } else ("— ENTRIES", "—")

    div(
      cls := s"slot${if (isOpen) " open" else ""}${if (isSelected) " selected" else ""}",
      styleAttr := s"top:${top}px;z-index:$z;transform:$transform",
      onMouseEnter --> { _ => hoveredIndex.set(Some(index)) },
      onMouseLeave --> { _ => hoveredIndex.set(None) },
      onClick --> { _ => selectProduct(product) },
      div(
        cls := "card",
        div(cls := "card-tint", styleAttr := s"background:$tint"),
        div(
          cls := "card-top",
          div(
            span(cls := "card-code mono", productCode(index)),
            div(cls := "card-name", product.name)
          ),
          span(cls := "card-market mono", product.market.toUpperCase)
        ),
        div(
          cls := "card-mid",
          div(cls := "card-meta mono", s"${product.project} · ${product.createdBy}"),
          div(cls := "card-desc", product.description)
        ),
        div(
          cls := "card-foot mono",
          span(entriesLabel),
          span(lastLabel)
        )
      )
    )
  }

  private def mainPanel: HtmlElement =
    main(
      child <-- selectedProduct.signal.map {
        case None          => emptyState
        case Some(product) => selectedView(product)
      }
    )

  private def emptyState: HtmlElement =
    div(
      cls := "empty-state",
      h1("Every product, and what changed in it."),
      p("Pull a card from the box to read its baseline and change log.")
    )

  private def selectedView(product: DataProduct): HtmlElement =
    div(
      div(
        cls := "sel-head",
        div(
          div(cls := "sel-code mono", productCode(indexOf(product))),
          h1(cls := "sel-title", product.name)
        ),
        button(cls := "btn-close", "Close", onClick --> { _ => selectedProduct.set(None) })
      ),
      div(
        cls := "stat-grid",
        statTile("Market", product.market),
        statTile("Project", product.project),
        statTile("Owner", product.createdBy),
        div(
          cls := "stat",
          div(cls := "stat-label mono", "Entries"),
          div(cls := "stat-value", child.text <-- changeLogs.signal.map(_.size.toString))
        )
      ),
      p(cls := "sel-desc", product.description),
      div(
        cls := "log-head",
        span(cls := "log-label mono", "Change log"),
        button(cls := "btn-accent", "Add entry", onClick.mapTo(true) --> showAddLog)
      ),
      children <-- changeLogs.signal.map { logs =>
        if (logs.isEmpty) List(p(cls := "log-empty", "No entries yet."))
        else logs.map(renderLogEntry)
      }
    )

  private def indexOf(product: DataProduct): Int =
    products.now().indexWhere(_.id == product.id) match {
      case -1 => 0
      case i  => i
    }

  private def statTile(labelText: String, value: String): HtmlElement =
    div(
      cls := "stat",
      div(cls := "stat-label mono", labelText),
      div(cls := "stat-value", value)
    )

  private def renderLogEntry(log: ChangeLog): HtmlElement =
    div(
      cls := "log-entry",
      div(cls := "log-date mono", log.requestedOn),
      div(
        div(cls := "log-change", log.changeDescription),
        div(cls := "log-detail", log.detailedDescription),
        div(cls := "log-by mono", log.requestedBy)
      )
    )

  // --- forms --------------------------------------------------------------

  private def field(labelText: String, target: Var[String]): HtmlElement =
    label(
      cls := "field",
      span(cls := "field-label mono", labelText),
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
        cls := "modal",
        h2("New data product"),
        div(
          cls := "field-grid",
          field("Name", name),
          field("Description", description),
          div(cls := "field-row-2", field("Market", market), field("Project", project)),
          field("Created by", createdBy)
        ),
        div(
          cls := "modal-actions",
          button(cls := "btn-plain", "Cancel", onClick.mapTo(false) --> showAddProduct),
          button(cls := "btn-accent", "Save product", onClick --> { _ => submit() })
        )
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
      cls := "panel",
      h2("Add log entry"),
      div(
        cls := "field-grid",
        field("Change", changeDescription),
        field("Requested by", requestedBy),
        field("Details", detailedDescription)
      ),
      div(
        cls := "panel-actions",
        button(cls := "btn-accent", "Save", onClick --> { _ => submit() }),
        button(cls := "btn-plain", "Cancel", onClick.mapTo(false) --> showAddLog)
      )
    )
  }
}
