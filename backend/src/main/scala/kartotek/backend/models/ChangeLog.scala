package kartotek.backend.models

import java.time.Instant

case class ChangeLog(
    productId: String,
    changeDescription: String, // e.g. "Planned refresh of the baseline"
    requestedAt: Instant,
    requestedBy: String, // e.g. "someone@email.com"
    detailedDescription: String // e.g. "Some details about the refresh"
)
