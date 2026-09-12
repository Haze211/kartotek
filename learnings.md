# Carried over from Kartotek_old

This repo started from the personal Scala template (Scala 3.3.8, sbt 1.12.14,
scalafmt, GitHub Actions CI) — those conventions were kept, and the two old
prototype repos were grafted on as the `backend` and `frontend` modules.

Leftover from the template and safe to delete once you're happy with the
build: `src/main/scala/Main.scala` and `src/test/scala/MainSpec.scala` at the
repo root. The root project is configured with empty source directories so
they no longer take part in the build.


## Kept

- **Domain shape.** `DataProduct` (name, description, market, project,
  createdAt, createdBy) and `ChangeLog` (productID, changeDescription,
  requestedAt, requestedBy, detailedDescription) — this matched the brief
  almost exactly. Added an explicit `id` field on `DataProduct` (it used to
  be generated ad hoc in the route handler instead of living on the model).
- **Mongo collection layout.** `data_products` and `change_log`, joined on
  `productID` — reused directly.
- **API shape.** GET/POST data products, GET/POST change log by product ID —
  same routes, now split into a proper routes/repository layering instead of
  one object doing everything.
- **Frontend visual/interaction design.** The stacked, staggered card
  sidebar (`.card-stack`, `.card-tab`, staggered `top` offsets) with
  click-to-expand into a change-log panel is the actual "card index"
  metaphor from the brief, done in Laminar — carried forward close to
  as-is.
- **Frontend toolchain.** Scala.js + Laminar + Vite
  (`@scala-js/vite-plugin-scalajs`) — kept as the base scaffolding.

## Rebuilt / changed

- Swapped `BlazeServerBuilder` for `EmberServerBuilder` (http4s deprecated
  Blaze).
- Split the old monolithic `Main.scala` into `routes/`, `repository/`,
  `config/`, and `models/` — the old version mixed HTTP routing, raw Mongo
  calls, and JSON handling in one file with no error handling.
- Mongo connection string now reads from `KARTOTEK_MONGO_URI` instead of a
  hardcoded `localhost:27017`.
- Bumped dependency versions (http4s, mongo4cats, circe, logback) to current
  releases.
- Merged the two separate old repos (`kartotek`, `katotek-ui`) into one sbt
  multi-project build (`backend`/`frontend`) so both share build tooling.

## Discarded

- `UnleashHttp4s.scala` — an unrelated http4s tutorial exercise (a
  Movie/Director example), no connection to this domain.
- `mongo_testss.scala` — a scratch file, half commented out, containing a
  chunk of copy-pasted sample code from the mongo4cats docs.
- The old mock data in `App.scala` (`changeLogsByProduct`, hardcoded
  `cards`) and the non-functional Add modal/slide-panel stubs — the
  frontend and backend never actually talked to each other in the old repo;
  that wiring still needs to be built.
