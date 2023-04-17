package uk.gov.justice.digital.controller

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
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
import uk.gov.justice.digital.test.Fixtures.domain2
import uk.gov.justice.digital.test.Fixtures.domain3
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

        val response = client.toBlocking().retrieve(HttpRequest.GET<String>("/domain"))

        assertEquals(emptyList<Domain>(), mapper.readValue<List<Domain>>(response))
        verify { mockDomainService.getDomains() }
    }

    @Test
    fun `GET of domain resource should return all domains`() {
        every { mockDomainService.getDomains(isNull()) } returns listOf(domain1, domain2, domain3)

        val response: String = client.toBlocking().retrieve(HttpRequest.GET<String>("/domain"))

        println("Got response: $response")

        assertEquals(listOf(domain1, domain2, domain3), mapper.readValue<List<Domain>>(response))
        verify { mockDomainService.getDomains() }
    }

    @Test
    fun `GET of domain with existing ID should return the domain with that ID`() {
        every { mockDomainService.getDomain(eq(domain1.id)) } returns domain1

        val response: String = client.toBlocking().retrieve(HttpRequest.GET<String>("/domain/${domain1.id}"))

        assertEquals(domain1, mapper.readValue<Domain>(response))
        verify { mockDomainService.getDomain(domain1.id) }
    }

    // TODO - this should return a 404
//    @Test
//    fun `GET of domain with non-existing ID should throw a HttpClientResponseException`() {
//        assertThrows(HttpClientResponseException::class.java) {
//            client.toBlocking().retrieve(HttpRequest.GET<String>("/domain/${UUID.randomUUID()}"))
//        }
//    }

//    @Test
//    fun `GET of domain with invalid ID should throw an IllegalArgumentException`() {
//        assertThrows(IllegalArgumentException::class.java) {
//            client.toBlocking().retrieve(HttpRequest.GET<String>("/domain/this-is-not-a-UUID}"))
//        }
//    }

    @Test
    fun `GET of domain with existing name should return the domain with that name`() {
        every { mockDomainService.getDomains(eq(domain1.name)) } returns listOf(domain1)

        val response: String = client.toBlocking().retrieve(HttpRequest.GET<String>("/domain?name=Domain%201"))

        assertEquals(listOf(domain1), mapper.readValue<List<Domain>>(response))
        verify { mockDomainService.getDomains(domain1.name) }
    }

    @Test
    fun `GET of domain with non-existing name should return an empty list`() {
        val nonExistingName = "foo"
        every { mockDomainService.getDomains(eq(nonExistingName)) } returns emptyList()

        val response: String = client.toBlocking().retrieve(HttpRequest.GET<String>("/domain?name=$nonExistingName"))

        assertEquals(emptyList<Domain>(), mapper.readValue<List<Domain>>(response))
        verify { mockDomainService.getDomains(nonExistingName) }
    }

}