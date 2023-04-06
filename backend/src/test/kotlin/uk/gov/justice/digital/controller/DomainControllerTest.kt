package uk.gov.justice.digital.controller

import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@MicronautTest
class DomainControllerTest {

    @Inject
    private lateinit var server: EmbeddedServer

    @Inject
    @field:Client("/")
    private lateinit var client: HttpClient

    @Test
    fun `GET domain should return all domains`() {
        val response: String = client.toBlocking().retrieve(HttpRequest.GET<String>("/domain"))
        assertTrue(response.contains("Some domain"))
    }

}