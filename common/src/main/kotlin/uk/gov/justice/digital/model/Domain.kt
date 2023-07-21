package uk.gov.justice.digital.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.micronaut.serde.annotation.Serdeable
import java.time.Instant
import java.util.*

@Serdeable
@JsonIgnoreProperties(ignoreUnknown = true)
data class Domain(
        val id: UUID,
        val name: String,
        val description: String?,
        val version: String?,
        val location: String?,
        val tags: Map<String, String>?,
        val owner: String?,
        val author: String?,
        val tables: List<Table>?,
        // The following fields are domain builder specific and can be omitted from the published domain.
        val status: Status = Status.DRAFT,
        // If the caller does not set the timestamps the repository will take care of them.
        val created: Instant? = null,
        val lastUpdated: Instant? = null,
)
