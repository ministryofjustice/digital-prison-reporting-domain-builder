package uk.gov.justice.digital.model

import com.fasterxml.jackson.annotation.JsonClassDescription

@JsonClassDescription
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
    val violations: List<String> = emptyList()
)
