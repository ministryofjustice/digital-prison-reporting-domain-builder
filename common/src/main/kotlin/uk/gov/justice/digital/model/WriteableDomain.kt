package uk.gov.justice.digital.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.micronaut.serde.annotation.Serdeable

@Serdeable
@JsonIgnoreProperties(ignoreUnknown = true)
data class WriteableDomain(
        val name: String,
        val description: String,
        val version: String,
        val location: String,
        val tags: Map<String, String> = emptyMap(),
        val owner: String,
        val author: String,
        val tables: List<Table> = emptyList(),
        // The following fields are domain builder specific and can be omitted from the published domain.
        val status: Status? = Status.DRAFT,
)
