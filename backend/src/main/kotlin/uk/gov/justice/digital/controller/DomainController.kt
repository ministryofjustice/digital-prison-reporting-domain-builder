package uk.gov.justice.digital.controller

import io.micronaut.http.MediaType.APPLICATION_JSON
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import uk.gov.justice.digital.model.Domain
import java.util.*

@Controller("/domain")
class DomainController {

    @Get(produces = [APPLICATION_JSON])
    fun getDomains(): List<Domain> {
        return listOf(Domain(
            id = UUID.randomUUID(),
            name = "Some domain",
            description = "Static test domain",
            version = "0.01",
            location = "/some-domain",
            tags = emptyMap(),
            owner = "someone@example.come",
            author = "someone@example.com",
            originator = "someone@example.com",
            tables = emptyList(),
        ))
   }

}