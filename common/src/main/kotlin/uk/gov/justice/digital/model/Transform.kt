package uk.gov.justice.digital.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.micronaut.serde.annotation.Serdeable

@Serdeable
@JsonIgnoreProperties(ignoreUnknown = true)
data class Transform(
    val viewText: String,
    val sources: List<String>,
)
