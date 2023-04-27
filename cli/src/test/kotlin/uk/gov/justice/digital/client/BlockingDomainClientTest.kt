package uk.gov.justice.digital.client

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Requires
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import uk.gov.justice.digital.model.Domain
import uk.gov.justice.digital.test.Fixtures.domain1
import uk.gov.justice.digital.test.Fixtures.domains

@MicronautTest(rebuildContext = true)
class BlockingDomainClientTest {

    @Test
    fun `getDomains should return empty list if there are no domains`() {
        val server = createServerForScenario(Scenarios.NO_DATA)
        val underTest = server.applicationContext.createBean(BlockingDomainClient::class.java)
        assertEquals(emptyList<Domain>(), underTest.getDomains().toList())
    }

    @Test
    fun `getDomainWithName should URL encode the name parameter ensuring a valid URL is constructed`() {
        val server = createServerForScenario(Scenarios.NO_DATA)
        val underTest = server.applicationContext.createBean(BlockingDomainClient::class.java)
        assertDoesNotThrow { underTest.getDomainWithName("This name has spaces which should be encoded") }
    }

    @Test
    fun `getDomainWithName should return null if no matching domain exists`() {
        val server = createServerForScenario(Scenarios.NO_DATA)
        val underTest = server.applicationContext.createBean(BlockingDomainClient::class.java)
        assertNull(underTest.getDomainWithName("some-name"))
    }

    @Test
    fun `getDomains should return all domains returned by the server`() {
        val server = createServerForScenario(Scenarios.HAPPY_PATH)
        val underTest = server.applicationContext.createBean(BlockingDomainClient::class.java)
        assertEquals(domains, underTest.getDomains().toList())
    }

    @Test
    fun `getDomainWithName should return a domain given a name`() {
        val server = createServerForScenario(Scenarios.HAPPY_PATH)
        val underTest = server.applicationContext.createBean(BlockingDomainClient::class.java)
        assertEquals(domain1, underTest.getDomainWithName("some-name"))
    }

    @Requires(property = TEST_SCENARIO, value = Scenarios.HAPPY_PATH)
    @Controller
    class HappyPathController {
        @Get("/domain")
        fun getAllDomains(): List<Domain> = domains
        @Get("/domain?name")
        fun getDomainWithName(@Suppress("UNUSED_PARAMETER") name: String): Domain = domain1
    }

    @Requires(property = TEST_SCENARIO, value = Scenarios.NO_DATA)
    @Controller
    class NoDataController {
        @Get("/domain")
        fun getAllDomains(): List<Domain> = emptyList()
        @Get("/domain?name")
        fun getDomainWithName(@Suppress("UNUSED_PARAMETER") name: String): Domain? = null
    }

    private fun createServerForScenario(scenario: String): EmbeddedServer {
        // Create an embedded server configured with the controller for the specified test scenario.
        val serverInstance = ApplicationContext.run(EmbeddedServer::class.java, mapOf(TEST_SCENARIO to scenario))
        // Ensure the client is correctly configured
        System.setProperty("http.client.url", "http://localhost:${serverInstance.port}")
        // Update configuration with embedded server port
        serverInstance.applicationContext.environment.refresh()
        return serverInstance
    }

    companion object {
        const val TEST_SCENARIO = "test.scenario"
        object Scenarios {
            const val HAPPY_PATH = "happyPath"
            const val NO_DATA = "noData"
        }
    }

}