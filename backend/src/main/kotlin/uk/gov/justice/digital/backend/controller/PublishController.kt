package uk.gov.justice.digital.backend.controller

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY
import io.micronaut.http.MediaType.APPLICATION_JSON
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Error
import io.micronaut.http.annotation.Post
import uk.gov.justice.digital.backend.service.DomainService
import uk.gov.justice.digital.backend.service.InvalidStatusException
import uk.gov.justice.digital.backend.service.PublishDomainNotFoundException
import uk.gov.justice.digital.model.Status

@Controller("/publish")
class PublishController(private val service: DomainService) {

    @Post(consumes = [APPLICATION_JSON], produces = [APPLICATION_JSON])
    fun publish(domainName: String, status: Status): HttpResponse<Unit> =
        service.publishDomain(domainName, status).let {
            HttpResponse.noContent()
        }

    @Error(exception = PublishDomainNotFoundException::class)
    fun handleDomainNotFoundException(): HttpResponse<Unit> = HttpResponse.notFound()

    @Error(exception = InvalidStatusException::class)
    fun handleInvalidStatusException(): HttpResponse<Unit> = HttpResponse.status(UNPROCESSABLE_ENTITY)

}