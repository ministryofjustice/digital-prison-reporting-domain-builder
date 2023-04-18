package uk.gov.justice.digital.controller

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.HttpStatus.OK
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
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
    fun `GET of domain with existing ID should return the domain with that ID`() {
        every { mockDomainService.getDomain(eq(domain1.id)) } returns domain1

        val response = get("/domain/${domain1.id}")

        assertEquals(OK, response.status)
        assertEquals(domain1, mapper.readValue<Domain>(response.body().orEmpty()))
        verify { mockDomainService.getDomain(domain1.id) }
    }

    // TODO - this should return a 404 - currently throws a HttpClientResponseException
    @Test
    fun `GET of domain with non-existing ID should return NOT_FOUND`() {
        every { mockDomainService.getDomain(any()) } returns null
        val response = get("/domain/${UUID.randomUUID()}")
        assertEquals(HttpStatus.NOT_FOUND, response.status())
    }

    // TODO - this should return a 400 - currently throws a HttpClientResponseException
    @Test
    fun `GET of domain with invalid ID should return BAD_REQUEST`() {
        val response = get("/domain/this-is-not-a-UUID")
        assertEquals(HttpStatus.BAD_REQUEST, response.status)
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

}