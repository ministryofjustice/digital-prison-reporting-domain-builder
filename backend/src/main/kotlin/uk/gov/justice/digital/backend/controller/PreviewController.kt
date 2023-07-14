package uk.gov.justice.digital.backend.controller

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import org.slf4j.LoggerFactory

@Controller("/preview")
class PreviewController {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Get
    fun requestPreview() {

    }

}