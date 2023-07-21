package uk.gov.justice.digital.backend.controller

import io.micronaut.http.MediaType.APPLICATION_JSON
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import org.slf4j.LoggerFactory
import uk.gov.justice.digital.backend.service.PreviewService
import uk.gov.justice.digital.model.Status
import java.util.*

@Controller("/preview")
class PreviewController(private val service: PreviewService) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Post(consumes = [APPLICATION_JSON], produces = [APPLICATION_JSON])
    fun requestPreview(domainName: String, status: Status? = null, limit: Int): List<Map<String, String>> {
        return service.preview(domainName, status, 10)
    }

}