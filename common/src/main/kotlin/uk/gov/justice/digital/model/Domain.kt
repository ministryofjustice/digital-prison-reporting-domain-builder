package uk.gov.justice.digital.model

import java.util.*

data class Domain(
    val id: UUID,
    val name: String,
    val description: String,
    val version: String,
    val location: String,
    val tags: Map<String, String>,
    // TODO - clarify what is meant by domain originator and whether it corresponds to either the owner or author.
    val owner: String,
    val author: String,
    val tables: List<Table>,
)
