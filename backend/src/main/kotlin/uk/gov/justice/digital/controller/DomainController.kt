package uk.gov.justice.digital.controller

import io.micronaut.http.MediaType.APPLICATION_JSON
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import uk.gov.justice.digital.model.Domain
import uk.gov.justice.digital.service.DomainService
import java.util.*

@Controller("/domain")
class DomainController(private val service: DomainService) {

    @Get("{?name}", produces = [APPLICATION_JSON])
    fun getDomains(name: String?): List<Domain> {
        return service.getDomains(name)
    }

    @Get("/{id}", produces = [APPLICATION_JSON])
    // TODO - how do we raise a 404 if nothing is found?
    fun getDomain(id: UUID): Domain? {
        return service.getDomain(id)
    }

}