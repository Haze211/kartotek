package kartotek.backend.models

import java.time.Instant
import java.util.UUID

case class DataProduct(
    id: String = UUID.randomUUID().toString,
    name: String, // e.g. "Egypt CPS Baseline"
    description: String, // e.g. "Cold start deployment of Egypt CPS"
    market: String, // e.g. "Egypt"
    project: String, // e.g. "CPS"
    createdAt: Instant = Instant.now(),
    createdBy: String // e.g. "ykhomenko@email.com"
)
