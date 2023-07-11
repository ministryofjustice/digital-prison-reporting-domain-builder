package uk.gov.justice.digital.backend.filter

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.every
import io.mockk.mockk
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.backend.service.DomainService
import uk.gov.justice.digital.headers.ApiKeyHeader
import uk.gov.justice.digital.test.Fixtures
import java.util.*

@MicronautTest
class ApiKeyFilterTest {

    @Inject
    private lateinit var server: EmbeddedServer

    @Inject
    @field:Client("/")
    private lateinit var client: HttpClient

    private val mockDomainService: DomainService = mockk()

    private val apiKeyHeader = ApiKeyHeader(Fixtures.TEST_API_KEY)

    @MockBean(DomainService::class)
    fun getMockDomainService() = mockDomainService

    @Test
    fun `request with a valid api key should be successful`() {
        every { mockDomainService.getDomains(any()) } returns emptyList()

        val request = HttpRequest.GET<String>("/domain")
            .header(apiKeyHeader.name, apiKeyHeader.value)

        val response =
            client
                .toBlocking()
                .exchange(request, String::class.java)

        assertEquals(response.status, HttpStatus.OK)
    }

    @Test
    fun `request with an invalid api key value should return unauthorized`() {
        every { mockDomainService.getDomains(any()) } returns emptyList()

        val request = HttpRequest.GET<String>("/domain")
            .header(apiKeyHeader.name, "this api key is not valid")

        val result = assertThrows(HttpClientResponseException::class.java) {
            client
                .toBlocking()
                .exchange(request, String::class.java)
        }

        assertEquals(result.status, HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `request with no api key header should return unauthorized`() {
        every { mockDomainService.getDomains(any()) } returns emptyList()

        val request = HttpRequest.GET<String>("/domain")

        val result = assertThrows(HttpClientResponseException::class.java) {
            client
                .toBlocking()
                .exchange(request, String::class.java)
        }

        assertEquals(result.status, HttpStatus.UNAUTHORIZED)
    }

}