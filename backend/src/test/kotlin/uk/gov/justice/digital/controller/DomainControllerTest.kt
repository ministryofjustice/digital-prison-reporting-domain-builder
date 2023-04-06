package uk.gov.justice.digital.controller

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.model.Domain
import uk.gov.justice.digital.service.StaticData.domain1
import uk.gov.justice.digital.service.StaticData.domain2
import uk.gov.justice.digital.service.StaticData.domain3
import java.lang.IllegalArgumentException
import java.util.*

@MicronautTest
class DomainControllerTest {

    // TODO - look into how micronaut handles de/ser
    private val mapper = jacksonObjectMapper()

    @Inject
    private lateinit var server: EmbeddedServer

    @Inject
    @field:Client("/")
    private lateinit var client: HttpClient

    @Test
    fun `GET of domain resource should return all domains`() {
        val response: String = client.toBlocking().retrieve(HttpRequest.GET<String>("/domain"))
        val parsed: List<Domain> = mapper.readValue(response)
        assertEquals(listOf(domain1, domain2, domain3), parsed)
    }

    @Test
    fun `GET of domain with existing ID should return the domain with that ID`() {
        val response: String = client.toBlocking().retrieve(HttpRequest.GET<String>("/domain/${domain1.id}"))
        val parsed: Domain = mapper.readValue(response)
        assertEquals(domain1, parsed)
    }

    // TODO - look into how to handle 4xx errors
    @Test
    fun `GET of domain with non-existing ID should return not found`() {
        assertThrows(HttpClientResponseException::class.java) {
            client.toBlocking().retrieve(HttpRequest.GET<String>("/domain/${UUID.randomUUID()}"))
        }
    }

    // TODO - look into how to handle 4xx errors
    @Test
    fun `GET of domain with invalid ID should return bad request`() {
        assertThrows(IllegalArgumentException::class.java) {
            client.toBlocking().retrieve(HttpRequest.GET<String>("/domain/this-is-not-a-UUID}"))
        }
    }

    @Test
    fun `GET of domain with existing name should return the domain with that name`() {
        val response: String = client.toBlocking().retrieve(HttpRequest.GET<String>("/domain?name=Domain%201"))
        val parsed: List<Domain> = mapper.readValue(response)
        assertEquals(listOf(domain1), parsed)
    }

    @Test
    fun `GET of domain with non-existing name should return not found`() {
        val response: String = client.toBlocking().retrieve(HttpRequest.GET<String>("/domain?name=foo"))
        val parsed: List<Domain> = mapper.readValue(response)
        assertEquals(emptyList<Domain>(), parsed)
    }

}