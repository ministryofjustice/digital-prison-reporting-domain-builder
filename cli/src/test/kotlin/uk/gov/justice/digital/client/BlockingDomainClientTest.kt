package uk.gov.justice.digital.client

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Requires
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import uk.gov.justice.digital.cli.client.BadRequestException
import uk.gov.justice.digital.cli.client.BlockingDomainClient
import uk.gov.justice.digital.cli.client.ConflictException
import uk.gov.justice.digital.cli.client.UnexpectedResponseException
import uk.gov.justice.digital.model.Domain
import uk.gov.justice.digital.model.Status
import uk.gov.justice.digital.model.WriteableDomain
import uk.gov.justice.digital.test.Fixtures.domain1
import uk.gov.justice.digital.test.Fixtures.domain2
import uk.gov.justice.digital.test.Fixtures.domains
import uk.gov.justice.digital.test.Fixtures.writeableDomain
import java.net.URI

@MicronautTest(rebuildContext = true)
class BlockingDomainClientTest {

    @Test
    fun `getDomains should return empty list if there are no domains`() {
        assertEquals(emptyList<Domain>(), createClientForScenario(Scenarios.NO_DATA).getDomains().toList())
    }

    @Test
    fun `getDomains should URL encode the name parameter ensuring a valid URL is constructed`() {
        assertDoesNotThrow {
            createClientForScenario(Scenarios.NO_DATA).getDomains("This name has spaces which should be encoded")
        }
    }

    @Test
    fun `getDomains should return an empty array if no matching domain exists`() {
        assertTrue(createClientForScenario(Scenarios.NO_DATA).getDomains("some-name").isEmpty())
    }

    @Test
    fun `getDomains call with no parameters should return all domains returned by the server`() {
        assertEquals(domains, createClientForScenario(Scenarios.HAPPY_PATH).getDomains().toList())
    }

    @Test
    fun `getDomains should return a domain given a name that exists`() {
        val result = createClientForScenario(Scenarios.HAPPY_PATH).getDomains("someone")
        assertEquals(1, result.size)
        assertEquals(domain1, result[0])
    }

    @Test
    fun `getDomains should return a domain where a domain exists for the given name and status`() {
        val result = createClientForScenario(Scenarios.HAPPY_PATH).getDomains("someone", Status.DRAFT)
        assertEquals(1, result.size)
        assertEquals(domain2, result[0])
    }

    @Test
    fun `getDomains should return an empty array if no matching domain exists for the given name and status`() {
        val result = createClientForScenario(Scenarios.NO_DATA).getDomains("someone", Status.DRAFT)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `createDomain should return the path to the new domain on success`() {
        val result = createClientForScenario(Scenarios.HAPPY_PATH).createDomain(writeableDomain)
        assertEquals("/domain/$FIXED_UUID", result)
    }

    @Test
    fun `createDomain should throw exception for conflict response`() {
        assertThrows(ConflictException::class.java) {
            createClientForScenario(Scenarios.CONFLICT).createDomain(writeableDomain)
        }
    }

    @Test
    fun `createDomain should throw exception for bad request response`() {
        assertThrows(BadRequestException::class.java) {
            createClientForScenario(Scenarios.BAD_REQUEST).createDomain(writeableDomain)
        }
    }

    @Test
    fun `createDomain should throw exception for internal server error response`() {
        assertThrows(UnexpectedResponseException::class.java) {
            createClientForScenario(Scenarios.INTERNAL_SERVER_ERROR).createDomain(writeableDomain)
        }
    }

    @Requires(property = TEST_SCENARIO, value = Scenarios.HAPPY_PATH)
    @Controller
    class HappyPathController {
        @Get("/domain{?name,status}")
        fun getDomains(name: String?, status: Status?): Array<Domain> =
            if (name.isNullOrEmpty() && status == null) domains.toTypedArray()
            else if (!name.isNullOrEmpty() && status == null) arrayOf(domain1)
            else arrayOf(domain2)
        @Post("/domain")
        fun createDomain(@Suppress("UNUSED_PARAMETER") domain: WriteableDomain): HttpResponse<Unit> =
            HttpResponse.created(URI.create("/domain/$FIXED_UUID"))
    }

    @Requires(property = TEST_SCENARIO, value = Scenarios.NO_DATA)
    @Controller()
    class NoDataController {
        @Get("/domain{?name,status}")
        fun getDomains(@Suppress("UNUSED_PARAMETER") name: String?,
                       @Suppress("UNUSED_PARAMETER") status: Status?): Array<Domain> = emptyArray()
    }

    @Requires(property = TEST_SCENARIO, value = Scenarios.BAD_REQUEST)
    @Controller()
    class BadRequestController {
        @Post("/domain")
        fun createDomain(@Suppress("UNUSED_PARAMETER") domain: WriteableDomain): HttpResponse<Unit> =
            HttpResponse.status(HttpStatus.BAD_REQUEST)
    }

    @Requires(property = TEST_SCENARIO, value = Scenarios.CONFLICT)
    @Controller()
    class ConflictController {
        @Post("/domain")
        fun createDomain(@Suppress("UNUSED_PARAMETER") domain: WriteableDomain): HttpResponse<Unit> =
            HttpResponse.status(HttpStatus.CONFLICT)
    }

    @Requires(property = TEST_SCENARIO, value = Scenarios.INTERNAL_SERVER_ERROR)
    @Controller()
    class InternalServerErrorController {
        @Post("/domain")
        fun createDomain(@Suppress("UNUSED_PARAMETER") domain: WriteableDomain): HttpResponse<Unit> =
            HttpResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
    }

    private fun createClientForScenario(scenario: String): BlockingDomainClient =
        createServerForScenario(scenario)
            .applicationContext
            .createBean(BlockingDomainClient::class.java)

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

        const val FIXED_UUID = "af66d434-9ea3-4aae-a7ea-044d3c6d2d40"

        const val TEST_SCENARIO = "test.scenario"

        object Scenarios {
            const val HAPPY_PATH = "happyPath"
            const val NO_DATA = "noData"
            const val CONFLICT = "conflict"
            const val BAD_REQUEST = "badRequest"
            const val INTERNAL_SERVER_ERROR = "internalServerError"
        }

    }

}