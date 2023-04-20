package uk.gov.justice.digital.model

import com.fasterxml.jackson.annotation.JsonClassDescription
import java.util.*

@JsonClassDescription
data class Domain(
    val id: UUID,
    val name: String,
    val description: String,
    val version: String,
    val location: String,
    val tags: Map<String, String> = emptyMap(),
    val owner: String,
    val author: String,
    val originator: String,
    val tables: List<Table> = emptyList(),
)
