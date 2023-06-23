package uk.gov.justice.digital.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.micronaut.serde.annotation.Serdeable

@Serdeable
@JsonIgnoreProperties(ignoreUnknown = true)
data class Table(
    val name: String,
    val description: String,
    val version: String,
    val location: String,
    val tags: Map<String, String> = emptyMap(),
    val owner: String,
    val author: String,
    val primaryKey: String,
    val transform: Transform,
    val mapping: Mapping,
    val violations: List<Violation> = emptyList()
)
