package uk.gov.justice.digital.backend.controller

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import org.slf4j.LoggerFactory
import uk.gov.justice.digital.backend.service.PreviewService

@Controller("/preview")
class PreviewController(private val service: PreviewService) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Get
    fun requestPreview(): List<Map<String, String>> {
        return service.preview("""
            SELECT *
            FROM nomis_offender_bookings
            WHERE comm_status = 'DET' AND "active_flag" = 'N'
            limit 10;
        """.trimIndent())
    }

}