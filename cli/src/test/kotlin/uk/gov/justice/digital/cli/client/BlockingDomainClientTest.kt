package uk.gov.justice.digital.cli.client

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Requires
import io.micronaut.http.HttpHeaders.USER_AGENT
import io.micronaut.http.HttpRequest
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
import uk.gov.justice.digital.cli.client.BlockingDomainClient
import uk.gov.justice.digital.headers.Header
import uk.gov.justice.digital.headers.Header.Companion.API_KEY_HEADER_NAME
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

    @Test
    fun `createDomain should set standard headers on requests`() {
        val client = createClientForScenario(Scenarios.VERIFY_HEADERS)
        // Get the session ID...
        val sessionId = client.createDomain(writeableDomain)
        // ...and verify that it's the same on successive requests
        assertEquals(sessionId, client.createDomain(writeableDomain))
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

    @Requires(property = TEST_SCENARIO, value = Scenarios.VERIFY_HEADERS)
    @Controller()
    class VerifyHeadersController {
        @Post("/domain")
        fun createDomain(request: HttpRequest<WriteableDomain>): HttpResponse<Unit> {

            val headers = request.headers

            val traceId = headers.get(Header.TRACE_ID_HEADER_NAME)
            val sessionId = headers.get(Header.SESSION_ID_HEADER_NAME)
            val userAgent = headers.get(USER_AGENT)
            val apiKey = headers.get(API_KEY_HEADER_NAME)

            val uuidLength = 36

            assertEquals(apiKey, TEST_API_KEY)
            assertEquals(uuidLength, traceId?.length)
            assertEquals(uuidLength, sessionId?.length)
            assertEquals(userAgent, "domain-builder-cli/v0.0.1")

            // Return the session ID in the location header so the client returns this to us.
            return HttpResponse.created(URI.create("SessionId:$sessionId"))
        }

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
        System.setProperty("http.client.apiKey", TEST_API_KEY)
        // Update configuration with embedded server port
        serverInstance.applicationContext.environment.refresh()
        return serverInstance
    }

    companion object {

        const val FIXED_UUID = "af66d434-9ea3-4aae-a7ea-044d3c6d2d40"
        const val TEST_API_KEY = "test-api-key"

        const val TEST_SCENARIO = "test.scenario"

        object Scenarios {
            const val HAPPY_PATH = "happyPath"
            const val NO_DATA = "noData"
            const val CONFLICT = "conflict"
            const val BAD_REQUEST = "badRequest"
            const val INTERNAL_SERVER_ERROR = "internalServerError"
            const val VERIFY_HEADERS = "verifyHeaders"
        }

    }

}