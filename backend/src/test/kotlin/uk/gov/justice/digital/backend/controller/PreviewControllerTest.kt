package uk.gov.justice.digital.backend.controller

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.serde.ObjectMapper
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.every
import io.mockk.mockk
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.backend.service.DomainNotFoundException
import uk.gov.justice.digital.backend.service.NoTablesInDomainException
import uk.gov.justice.digital.backend.service.PreviewService
import uk.gov.justice.digital.headers.Header
import uk.gov.justice.digital.model.Status
import uk.gov.justice.digital.test.Fixtures

@MicronautTest
class PreviewControllerTest {

    @Inject
    private lateinit var server: EmbeddedServer

    @Inject
    @field:Client("/")
    private lateinit var client: HttpClient

    @Inject
    private lateinit var objectMapper: ObjectMapper

    private val mockPreviewService: PreviewService = mockk()

    @MockBean(PreviewService::class)
    fun getMockPreviewService() = mockPreviewService

    @Test
    fun `request for valid domain returns a HTTP 200 response containing the preview data`() {
        val fakeData = listOf(
            mapOf("foo" to "1"),
            mapOf("bar" to "1"),
            mapOf("baz" to "1"),
        )

        every { mockPreviewService.preview("someDomain", Status.DRAFT, 10) } returns fakeData

        val response = sendRequest()

        assertEquals(HttpStatus.OK, response.status)

        val responseBody = response.body()
        val fakeDataJson = objectMapper.writeValueAsString(fakeData)

        assertEquals(fakeDataJson, responseBody)
    }

    @Test
    fun `request for domain that does not exist returns a HTTP 404`() {
        every { mockPreviewService.preview(any(), any(), any()) } throws DomainNotFoundException("Error")
        val response = assertThrows(HttpClientResponseException::class.java) { sendRequest() }
        assertEquals(HttpStatus.NOT_FOUND, response.status)
    }

    @Test
    fun `request that attempts to preview a domain with no tables returns a HTTP 422`() {
        every { mockPreviewService.preview(any(), any(), any()) } throws NoTablesInDomainException("Error")
        val response = assertThrows(HttpClientResponseException::class.java) { sendRequest() }
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.status)

    }

    private fun sendRequest(): HttpResponse<String> =
        client
            .toBlocking()
            .exchange(
                HttpRequest
                    .POST("/preview",
                        """
                                {
                                    "domainName": "someDomain",
                                    "status": "DRAFT",
                                    "limit": 10
                                }
                            """.trimIndent()
                    )
                    .contentType(MediaType.APPLICATION_JSON_TYPE)
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .header(Header.API_KEY_HEADER_NAME, Fixtures.TEST_API_KEY),
                String::class.java
            )
}