package uk.gov.justice.digital.client

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Requires
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import uk.gov.justice.digital.model.Domain
import uk.gov.justice.digital.test.Fixtures
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
    fun `getDomains should URL encode the name parameter ensuring a valid URL is constructed`() {
        val server = createServerForScenario(Scenarios.NO_DATA)
        val underTest = server.applicationContext.createBean(BlockingDomainClient::class.java)
        assertDoesNotThrow { underTest.getDomains("This name has spaces which should be encoded") }
    }

    @Test
    fun `getDomains should return an empty array if no matching domain exists`() {
        val server = createServerForScenario(Scenarios.NO_DATA)
        val underTest = server.applicationContext.createBean(BlockingDomainClient::class.java)
        assertTrue(underTest.getDomains("some-name").isEmpty())
    }

    @Test
    fun `getDomains call with no parameters should return all domains returned by the server`() {
        val server = createServerForScenario(Scenarios.HAPPY_PATH)
        val underTest = server.applicationContext.createBean(BlockingDomainClient::class.java)
        assertEquals(domains, underTest.getDomains().toList())
    }

    @Test
    fun `getDomains should return a domain given a name that exists`() {
        val server = createServerForScenario(Scenarios.HAPPY_PATH)
        val underTest = server.applicationContext.createBean(BlockingDomainClient::class.java)
        val result = underTest.getDomains("someone")
        assertEquals(1, result.size)
        assertEquals(domain1, result[0])
    }

    // TODO - scenario that includes status
    @Requires(property = TEST_SCENARIO, value = Scenarios.HAPPY_PATH)
    @Controller
    class HappyPathController {
        @Get("/domain{?name}")
        fun getDomains(name: String?): Array<Domain> =
            if (name.isNullOrEmpty()) domains.toTypedArray()
            else arrayOf(domain1)
    }

    @Requires(property = TEST_SCENARIO, value = Scenarios.NO_DATA)
    @Controller()
    class NoDataController {
        @Get("/domain{?name}")
        fun getDomains(@Suppress("UNUSED_PARAMETER") name: String?): Array<Domain> = emptyArray()
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