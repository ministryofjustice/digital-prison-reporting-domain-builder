package uk.gov.justice.digital.backend.controller

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.every
import io.mockk.mockk
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.backend.service.DomainNotFoundException
import uk.gov.justice.digital.backend.service.DomainService
import uk.gov.justice.digital.backend.service.InvalidStatusException
import uk.gov.justice.digital.headers.Header
import uk.gov.justice.digital.model.Status
import uk.gov.justice.digital.test.Fixtures

@MicronautTest
class PublishControllerTest {

    @Inject
    private lateinit var server: EmbeddedServer

    @Inject
    @field:Client("/")
    private lateinit var client: HttpClient

    private val mockDomainService: DomainService = mockk()

    @MockBean(DomainService::class)
    fun getMockDomainService() = mockDomainService

    @Test
    fun `request for valid domain returns a HTTP 204 response`() {
        every { mockDomainService.publishDomain(any(), any()) } returns Unit
        val response = sendRequest()
        assertEquals(HttpStatus.NO_CONTENT, response.status)
    }

    @Test
    fun `request for domain that does not exist returns a HTTP 404`() {
        every { mockDomainService.publishDomain(any(), any()) } throws DomainNotFoundException("Error")
        val response = assertThrows(HttpClientResponseException::class.java) { sendRequest() }
        assertEquals(HttpStatus.NOT_FOUND, response.status)
    }

    @Test
    fun `attempt to publish a domain that is already published returns a HTTP 422`() {
        every { mockDomainService.publishDomain(any(), any()) } throws InvalidStatusException("Cannot publish PUBLISHED domain")
        val response = assertThrows(HttpClientResponseException::class.java) { sendRequest(Status.PUBLISHED) }
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.status)
    }

    private fun sendRequest(status: Status = Status.DRAFT): HttpResponse<String> =
        client
            .toBlocking()
            .exchange(
                HttpRequest
                    .POST("/publish",
                        """
                                {
                                    "domainName": "someDomain",
                                    "status": "DRAFT"
                                }
                            """.trimIndent()
                    )
                    .contentType(MediaType.APPLICATION_JSON_TYPE)
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .header(Header.API_KEY_HEADER_NAME, Fixtures.TEST_API_KEY),
                String::class.java
            )
}