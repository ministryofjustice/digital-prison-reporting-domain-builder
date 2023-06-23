package uk.gov.justice.digital.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.micronaut.serde.annotation.Serdeable

@Serdeable
@JsonIgnoreProperties(ignoreUnknown = true)
data class Mapping(
    val viewText: String,
)