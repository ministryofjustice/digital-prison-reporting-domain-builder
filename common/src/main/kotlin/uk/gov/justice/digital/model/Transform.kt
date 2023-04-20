package uk.gov.justice.digital.model

import com.fasterxml.jackson.annotation.JsonClassDescription

@JsonClassDescription
data class Transform(
    val viewText: String,
    val sources: List<String>,
)
