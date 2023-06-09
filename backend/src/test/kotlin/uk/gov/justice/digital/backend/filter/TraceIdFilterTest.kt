package uk.gov.justice.digital.backend.filter

import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.every
import io.mockk.mockk
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import uk.gov.justice.digital.backend.service.DomainService
import uk.gov.justice.digital.headers.ApiKeyHeader
import uk.gov.justice.digital.headers.Header
import uk.gov.justice.digital.headers.SessionIdHeader
import uk.gov.justice.digital.headers.TraceIdHeader
import uk.gov.justice.digital.test.Fixtures
import java.util.*

@MicronautTest
class TraceIdFilterTest {

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
    fun `request with trace and session IDs should result in a response with the same trace headers`() {
        every { mockDomainService.getDomains(any()) } returns emptyList()

        val traceId = TraceIdHeader()
        val sessionId = SessionIdHeader()

        val request = HttpRequest.GET<String>("/domain")
            .header(traceId.name, traceId.value)
            .header(sessionId.name, sessionId.value)
            .header(apiKeyHeader.name, apiKeyHeader.value)

        val response =
            client
                .toBlocking()
                .exchange(request, String::class.java)

        assertEquals(traceId.value, response.header(traceId.name))
        assertEquals(sessionId.value, response.header(sessionId.name))
    }

    @Test
    fun `request with no trace IDs should result in a response with a traceId header`() {
        every { mockDomainService.getDomains(any()) } returns emptyList()

        val request = HttpRequest.GET<String>("/domain")
            .header(apiKeyHeader.name, apiKeyHeader.value)

        val response =
            client
                .toBlocking()
                .exchange(request, String::class.java)

        assertNotNull(response.header(Header.TRACE_ID_HEADER_NAME))
        assertDoesNotThrow { UUID.fromString(response.header(Header.TRACE_ID_HEADER_NAME)) }
        assertNull(response.header(Header.SESSION_ID_HEADER_NAME))
    }

}