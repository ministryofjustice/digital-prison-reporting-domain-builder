package uk.gov.justice.digital.backend.controller

import io.micronaut.http.MediaType.APPLICATION_JSON
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import uk.gov.justice.digital.backend.service.PreviewService
import uk.gov.justice.digital.model.Status
import java.util.*

@Controller("/preview")
class PreviewController(private val service: PreviewService) {

    @Post(consumes = [APPLICATION_JSON], produces = [APPLICATION_JSON])
    fun runPreview(domainName: String, status: Status? = null, limit: Int): List<Map<String, String>> {
        println("Request params: $domainName $status $limit")
        val result = service.preview(domainName, status, limit)
        println("Returning result: $result")
        return result
    }

}