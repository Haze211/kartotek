# kartotek

A card-index for data product baselines. Each card is a baseline; under it
sits a running log of who asked for what, when, and why — the requests that
today get lost across emails and chats.

## Stack

- Scala 3.3.8 (LTS), sbt 1.12.14
- Backend: http4s (Ember) + cats-effect + MongoDB via mongo4cats
- Frontend: Scala.js + Laminar, bundled with Vite
- MUnit for testing, scalafmt for formatting, GitHub Actions CI

## Layout

```
backend/    http4s API — config, models, repository, routes
frontend/   Scala.js + Laminar UI — App.scala, ApiClient.scala, models
```

## Running

Backend (needs MongoDB on `localhost:27017`, or set `KARTOTEK_MONGO_URI`):

```bash
sbt backend/run     # serves on :8080
```

Frontend:

```bash
cd frontend
npm install
npm run dev         # serves on :5173
```

Vite drives the Scala.js build through `@scala-js/vite-plugin-scalajs`, so
`npm run dev` picks up Scala changes too.

Other useful commands:

```bash
sbt test            # all modules
sbt scalafmtAll     # format
```

### Open in IntelliJ

1. `File > Open` and select this project's root folder.
2. Let the Scala plugin import the sbt build (accept the popup, or `sbt` tool
   window > refresh). Both `backend` and `frontend` appear as modules.

## API

| Method | Path                                      | Purpose                      |
|--------|-------------------------------------------|------------------------------|
| GET    | `/data-products`                          | list all baselines           |
| GET    | `/data-products/:id`                      | one baseline                 |
| POST   | `/data-products`                          | create a baseline            |
| GET    | `/data-products/:id/change-log`           | change log for a baseline    |
| POST   | `/data-products/:id/change-log`           | append a change log entry    |

Configuration comes from the environment: `KARTOTEK_MONGO_URI`,
`KARTOTEK_MONGO_DB`, `KARTOTEK_HTTP_PORT`.

## History

Grown out of an earlier two-repo prototype; see [learnings.md](learnings.md)
for what was carried over, rebuilt, and dropped.
