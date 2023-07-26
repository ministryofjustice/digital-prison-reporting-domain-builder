package uk.gov.justice.digital.backend.controller

import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType.APPLICATION_JSON
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Error
import io.micronaut.http.annotation.Post
import uk.gov.justice.digital.backend.service.DomainNotFoundException
import uk.gov.justice.digital.backend.service.NoTablesInDomainException
import uk.gov.justice.digital.backend.service.PreviewService
import uk.gov.justice.digital.model.Status
import java.util.*

@Controller("/preview")
class PreviewController(private val service: PreviewService) {

    @Post(consumes = [APPLICATION_JSON], produces = [APPLICATION_JSON])
    fun runPreview(domainName: String, status: Status, limit: Int): List<Map<String, String>> =
        service.preview(domainName, status, limit)

    @Error(exception = DomainNotFoundException::class)
    fun handleDomainNotFoundException(): HttpResponse<Unit> = HttpResponse.notFound()

    @Error(exception = NoTablesInDomainException::class)
    fun handleNoTablesInDomainException(): HttpResponse<Unit> = HttpResponse.unprocessableEntity()

}