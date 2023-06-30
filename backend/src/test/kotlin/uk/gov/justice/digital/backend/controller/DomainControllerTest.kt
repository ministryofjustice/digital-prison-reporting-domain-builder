package uk.gov.justice.digital.backend.controller

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus.*
import io.micronaut.http.MediaType
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
import uk.gov.justice.digital.backend.repository.DuplicateKeyException
import uk.gov.justice.digital.backend.service.DomainService
import uk.gov.justice.digital.backend.service.InvalidSparkSqlException
import uk.gov.justice.digital.backend.validator.InvalidSparkSqlResult
import uk.gov.justice.digital.model.Domain
import uk.gov.justice.digital.model.Status
import uk.gov.justice.digital.test.Fixtures.domain1
import uk.gov.justice.digital.test.Fixtures.domains
import uk.gov.justice.digital.test.Fixtures.writeableDomain
import uk.gov.justice.digital.test.Fixtures.writeableDomainWithInvalidMappingSql
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
    fun getMockDomainService() = mockDomainService

    @Test
    fun `GET of domain resource should return empty list if no domains exist`() {
        every { mockDomainService.getDomains(any())} returns emptyList()

        val response = get("/domain")

        assertEquals(OK, response.status)
        assertEquals(emptyList<Domain>(), response.parsedBody())

        verify { mockDomainService.getDomains() }
    }

    @Test
    fun `GET of domain resource should return all domains`() {
        every { mockDomainService.getDomains(isNull()) } returns domains

        val response = get("/domain")

        assertEquals(OK, response.status)
        assertEquals(domains, response.parsedBody<List<Domain>>())

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
        assertEquals(domain1, response.parsedBody<Domain>())

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
        assertEquals(listOf(domain1), response.parsedBody<List<Domain>>())

        verify { mockDomainService.getDomains(domain1.name) }
    }

    @Test
    fun `GET of domain with non-existing name should return an empty list`() {
        val nonExistingName = "foo"

        every { mockDomainService.getDomains(eq(nonExistingName)) } returns emptyList()

        val response = get("/domain?name=$nonExistingName")

        assertEquals(OK, response.status)
        assertEquals(emptyList<Domain>(), response.parsedBody())

        verify { mockDomainService.getDomains(nonExistingName) }
    }

    @Test
    fun `GET of domain with status should return any domains with that status`() {
        every { mockDomainService.getDomains(status = eq(Status.DRAFT)) } returns listOf(domain1)

        val response = get("/domain?status=DRAFT")

        assertEquals(OK, response.status)
        assertEquals(listOf(domain1), response.parsedBody<List<Domain>>())

        verify { mockDomainService.getDomains(status = eq(Status.DRAFT)) }
    }

    @Test
    fun `GET of domain with name and status should return a matching domain where it exists`() {
        every { mockDomainService.getDomains(eq(domain1.name), eq(domain1.status)) } returns listOf(domain1)

        val response = get("/domain?name=Domain%201&status=DRAFT")

        assertEquals(OK, response.status)
        assertEquals(listOf(domain1), response.parsedBody<List<Domain>>())

        verify { mockDomainService.getDomains(eq(domain1.name), eq(domain1.status)) }
    }

    @Test
    fun `POST of a valid domain should return a HTTP 201 with a location header pointing to the created resource`() {
        val uuid = UUID.randomUUID()
        every { mockDomainService.createDomain(any()) } returns uuid

        val response = post("/domain", jacksonObjectMapper().writeValueAsString(writeableDomain))

        assertEquals(CREATED, response.status)
        assertEquals("/domain/$uuid", response.header("Location"))
    }

    @Test
    fun `POST of an invalid domain should return a HTTP 400`() {
        val responseException = assertThrows(HttpClientResponseException::class.java) { post("/domain", "{}") }
        assertEquals(BAD_REQUEST, responseException.status)
    }

    @Test
    fun `POST of duplicate domain returns HTTP 409`() {
        every { mockDomainService.createDomain(any()) } throws DuplicateKeyException("Duplicate key error", RuntimeException())

        val responseException = assertThrows(HttpClientResponseException::class.java) {
            post("/domain", jacksonObjectMapper().writeValueAsString(writeableDomain))
        }

        assertEquals(CONFLICT, responseException.status)
    }

    @Test
    fun `POST of domain with invalid SQL should return a HTTP 400`() {
        every { mockDomainService.createDomain(any()) } throws InvalidSparkSqlException(InvalidSparkSqlResult("Bad SQL"))
        val responseException = assertThrows(HttpClientResponseException::class.java) {
            post("/domain", jacksonObjectMapper().writeValueAsString(writeableDomainWithInvalidMappingSql))
        }
        assertEquals(BAD_REQUEST, responseException.status)
    }

    private fun get(location: String): HttpResponse<String> =
        client
            .toBlocking()
            .exchange(HttpRequest.GET<String>(location), String::class.java)

    private fun post(location: String, requestBody: String): HttpResponse<String> =
        client
            .toBlocking()
            .exchange(
                HttpRequest
                    .POST(location, requestBody)
                    .contentType(MediaType.APPLICATION_JSON_TYPE)
            )

    private fun getAndCatchResponseException(location: String) =
        assertThrows(HttpClientResponseException::class.java) { get(location) }

    private inline fun <reified T> HttpResponse<String>.parsedBody(): T {
        return mapper.readValue<T>(this.body().orEmpty())
    }

}