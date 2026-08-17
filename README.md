# scala-template

A minimal starter template for new Scala projects. Clone it, rename the package/artifact, and start coding.

## Stack

- Scala 3.3.8 (LTS)
- sbt 1.12.14
- [MUnit](https://scalameta.org/munit/) for testing
- scalafmt for formatting
- GitHub Actions CI (format check + tests on every push/PR)

## Getting started

```bash
sbt run    # run the app
sbt test   # run tests
sbt scalafmtAll   # format the code
```

### Open in IntelliJ

1. `File > Open` and select this project's root folder.
2. Let the Scala plugin import the sbt build (accept the popup, or `sbt` tool window > refresh).
3. Run `Main` via the green gutter icon, or use the `sbt shell` tool window.

## Using this as a template for a new project

1. Rename the repo / clone it under the new project name.
2. Update `name` and `organization` in [build.sbt](build.sbt).
3. Replace [src/main/scala/Main.scala](src/main/scala/Main.scala) with your actual code.
