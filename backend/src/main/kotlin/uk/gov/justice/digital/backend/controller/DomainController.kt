package uk.gov.justice.digital.backend.controller

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus.*
import io.micronaut.http.MediaType.APPLICATION_JSON
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Error
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import uk.gov.justice.digital.backend.repository.DuplicateKeyException
import uk.gov.justice.digital.backend.service.DomainService
import uk.gov.justice.digital.backend.service.InvalidSparkSqlException
import uk.gov.justice.digital.model.Domain
import uk.gov.justice.digital.model.Status
import uk.gov.justice.digital.model.WriteableDomain
import java.net.URI
import java.util.*

@Controller("/domain")
class DomainController(private val service: DomainService) {

    @Get("{?name,status}", produces = [APPLICATION_JSON])
    fun getDomains(name: String?, status: Status?): List<Domain> {
        return service.getDomains(name, status)
    }

    @Get("/{id}", produces = [APPLICATION_JSON])
    fun getDomain(id: UUID): Domain? {
        return service.getDomain(id)
    }

    @Post(consumes = [APPLICATION_JSON])
    fun createDomain(domain: WriteableDomain): HttpResponse<Unit>? {
        val domainId = service.createDomain(domain)
        return HttpResponse.created(URI.create("/domain/$domainId"))
    }

    @Error(exception = DuplicateKeyException::class)
    fun duplicateKeyErrorHandler(): HttpResponse<Unit> {
        return HttpResponse.status(CONFLICT)
    }

    @Error(exception = InvalidSparkSqlException::class)
    fun invalidSparkSqlErrorHandler(): HttpResponse<Unit> {
        return HttpResponse.status(BAD_REQUEST)
    }

}