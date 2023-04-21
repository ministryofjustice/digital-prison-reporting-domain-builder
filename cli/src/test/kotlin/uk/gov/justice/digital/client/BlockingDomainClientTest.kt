package uk.gov.justice.digital.client

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.model.Domain
import uk.gov.justice.digital.test.Fixtures.domains

@MicronautTest(rebuildContext = true)
class BlockingDomainClientTest {

    @Inject
    private lateinit var server: EmbeddedServer

    @BeforeEach
    fun setServerUri() {
        println("Setting ${server.port} based url on properties")
        System.setProperty("micronaut.http.services.domain.url", "http://localhost:${server.port}")
        println("Refreshing environment config")
        server.applicationContext.environment.refresh()
        println("before each done")
    }

    @Test
    fun `getDomains should return all domains returned by the server`() {
        val client = server.applicationContext.createBean(BlockingDomainClient::class.java)
        val result = client.getDomains().toList()
        val expected = domains
        assertEquals(expected, result)
    }

    @Controller
    class FakeDomainServer {
        @Get("/domain")
        fun getAllDomains(): List<Domain> = domains
    }

}