package uk.gov.justice.digital.controller

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.HttpStatus.*
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.model.Domain
import uk.gov.justice.digital.service.DomainService
import uk.gov.justice.digital.test.Fixtures.domain1
import uk.gov.justice.digital.test.Fixtures.domains
import java.lang.RuntimeException
import java.util.*

@MicronautTest
class DomainControllerTest {

    private val mapper = jacksonObjectMapper()

    @Inject
    private lateinit var server: EmbeddedServer

    @Inject
    @field:Client("/")
    private lateinit var client: HttpClient

    private val mockDomainService: DomainService = mockk()

    @MockBean(DomainService::class)
    fun createMockDomainService() = mockDomainService

    @Test
    fun `GET of domain resource should return empty list if no domains exist`() {
        every { mockDomainService.getDomains(any())} returns emptyList()

        val response = get("/domain")

        assertEquals(OK, response.status)
        assertEquals(emptyList<Domain>(), mapper.readValue<List<Domain>>(response.body().orEmpty()))

        verify { mockDomainService.getDomains() }
    }

    @Test
    fun `GET of domain resource should return all domains`() {
        every { mockDomainService.getDomains(isNull()) } returns domains

        val response = get("/domain")

        assertEquals(OK, response.status)
        assertEquals(domains, mapper.readValue<List<Domain>>(response.body().orEmpty()))

        verify { mockDomainService.getDomains() }
    }

    @Test
    fun `GET of domain resource should return INTERNAL_SERVER_ERROR if an unexpected error occurs`() {
        every { mockDomainService.getDomains(any()) } throws RuntimeException("Something unexpected...")

        val responseException = getAndCatchResponseException("/domain")

        assertEquals(INTERNAL_SERVER_ERROR, responseException.status)

        verify { mockDomainService.getDomains() }
    }

    @Test
    fun `GET of domain with existing ID should return the domain with that ID`() {
        every { mockDomainService.getDomain(eq(domain1.id)) } returns domain1

        val response = get("/domain/${domain1.id}")

        assertEquals(OK, response.status)
        assertEquals(domain1, mapper.readValue<Domain>(response.body().orEmpty()))

        verify { mockDomainService.getDomain(domain1.id) }
    }

    @Test
    fun `GET of domain with non-existing ID should return NOT_FOUND`() {
        every { mockDomainService.getDomain(any()) } returns null

        val responseException = getAndCatchResponseException("/domain/${UUID.randomUUID()}")

        assertEquals(NOT_FOUND, responseException.status)

        verify { mockDomainService.getDomain(any()) }
    }

    @Test
    fun `GET of domain with invalid ID should return BAD_REQUEST`() {
        val responseException = getAndCatchResponseException("/domain/this-is-not-a-UUID")

        assertEquals(BAD_REQUEST, responseException.status)
        // UUID validation is handled in the routing later so the bad UUID should never be passed to the service.
        verify(inverse = true) { mockDomainService.getDomain(any()) }
    }

    @Test
    fun `GET of domain should return INTERNAL_SERVER_ERROR if an unexpected exception occurs`() {
        every { mockDomainService.getDomain(any()) } throws RuntimeException("Something unexpected...")

        val responseException = getAndCatchResponseException("/domain/${UUID.randomUUID()}")

        assertEquals(INTERNAL_SERVER_ERROR, responseException.status)

        verify { mockDomainService.getDomain(any()) }
    }

    @Test
    fun `GET of domain with existing name should return the domain with that name`() {
        every { mockDomainService.getDomains(eq(domain1.name)) } returns listOf(domain1)

        val response = get("/domain?name=Domain%201")

        assertEquals(OK, response.status)
        assertEquals(listOf(domain1), mapper.readValue<List<Domain>>(response.body().orEmpty()))

        verify { mockDomainService.getDomains(domain1.name) }
    }

    @Test
    fun `GET of domain with non-existing name should return an empty list`() {
        val nonExistingName = "foo"

        every { mockDomainService.getDomains(eq(nonExistingName)) } returns emptyList()

        val response = get("/domain?name=$nonExistingName")

        assertEquals(OK, response.status)
        assertEquals(emptyList<Domain>(), mapper.readValue<List<Domain>>(response.body().orEmpty()))

        verify { mockDomainService.getDomains(nonExistingName) }
    }

    private fun get(location: String): HttpResponse<String> =
        client
            .toBlocking()
            .exchange(HttpRequest.GET<String>(location), String::class.java)

    private fun getAndCatchResponseException(location: String) =
        assertThrows(HttpClientResponseException::class.java) { get(location) }

}