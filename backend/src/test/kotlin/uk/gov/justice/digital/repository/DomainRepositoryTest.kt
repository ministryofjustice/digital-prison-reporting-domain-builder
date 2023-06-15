package uk.gov.justice.digital.repository

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import uk.gov.justice.digital.model.Domain
import uk.gov.justice.digital.model.Status
import uk.gov.justice.digital.test.Fixtures.domain1
import uk.gov.justice.digital.test.Fixtures.domain2
import uk.gov.justice.digital.test.Fixtures.domain3
import uk.gov.justice.digital.test.Fixtures.publishedDomain1
import uk.gov.justice.digital.test.time.FixedClock
import java.time.Instant
import java.util.*

// This test needs a local docker daemon to be running.
// Set disabledWithoutDocker = true on the `@TestContainers` annotation to skip
// these tests if there is no daemon running locally.
@Testcontainers
class DomainRepositoryTest {

    companion object {
        @Container
        val postgresContainer: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:15.2")
            .withDatabaseName("test_domain_registry")
            .withUsername("postgres")
            .withPassword("postgres")
            .waitingFor(Wait.forListeningPort())
            .withReuse(true)

        val dataSource by lazy {
            val d = PGSimpleDataSource()
            d.setUrl(postgresContainer.jdbcUrl)
            d.user = "postgres"
            d.password = "postgres"
            d
        }

        @JvmStatic
        @BeforeAll
        fun applyMigrations() {
            Flyway
                .configure()
                .dataSource(dataSource)
                .load()
                .migrate()
        }
    }

    @BeforeEach
    fun truncateDomainTable() {
        dataSource
            .connection
            .createStatement()
            .executeUpdate("truncate table domain")
    }

    private val fixedClock = FixedClock().clock

    private val underTest by lazy { DomainRepository(dataSource, FixedClock()) }

    @Test
    fun `createDomain should succeed for a successful insert`() {
        assertDoesNotThrow { underTest.createDomain(domain1) }
    }

    @Test
    fun `createDomain should set timestamps where none are provided`() {
        underTest.createDomain(domain1)
        val retrievedDomain = underTest.getDomain(domain1.id)
        assertEquals(fixedClock.instant(), retrievedDomain?.created)
        assertEquals(fixedClock.instant(), retrievedDomain?.lastUpdated)
    }

    @Test
    fun `createDomain should use provided timestamps where provided`() {
        val ts = fixedClock.instant().plusSeconds(55)
        val domainWithTimestamps = domain1.copy(
                created = ts,
                lastUpdated = ts,
        )
        underTest.createDomain(domainWithTimestamps)
        assertEquals(domainWithTimestamps, underTest.getDomain(domainWithTimestamps.id))
    }

    @Test
    fun `createDomain should throw an exception for a failed insert`() {
        underTest.createDomain(domain1)
        assertThrows(CreateFailedException::class.java) {
            // This second insert attempt should fail since we're trying to insert a duplicate
            underTest.createDomain(domain1)
        }
    }

    @Test
    fun `createDomain should allow a domain name to be used again given a different state`() {
        underTest.createDomain(domain1)
        assertDoesNotThrow {
            // This second assert should succeed
            underTest.createDomain(publishedDomain1)
        }
    }

    @Test
    fun getDomainShouldReturnADomainWhereADomainForTheUUIDExists() {
        underTest.createDomain(domain1)
        val retrievedDomain = underTest.getDomain(domain1.id)
        assertNotNull(retrievedDomain)
        // Verify that the timestamps are set
        assertNotNull(retrievedDomain?.created, "created timestamp should be set")
        assertNotNull(retrievedDomain?.lastUpdated, "lastUpdated timestamp should be set")
        // For comparison with the fixture set the timestamps to null.
        assertEquals(domain1, retrievedDomain?.copy(created = null, lastUpdated = null))
    }

    @Test
    fun `getDomain should return null where no matching Domains exist`() {
        underTest.createDomain(domain1)
        assertNull(underTest.getDomain(UUID.randomUUID()))
    }

    @Test
    fun `getDomains should return all Domains where no arguments provided`() {
        val domains = listOf(domain1, domain2, domain3)
        domains.forEach { underTest.createDomain(it) }
        // After creation all domains will also have a created and lastupdated timestamp.
        val expectedDomains = domains.map { it.setTimestamps(fixedClock.instant()) }
        assertEquals(expectedDomains, underTest.getDomains())
    }

    @Test
    fun `getDomains should return a single domain where a name is specified which matches an existing Domain`() {
        listOf(domain1, domain2, domain3).forEach { underTest.createDomain(it) }
        val expected = domain3.setTimestamps(fixedClock.instant())
        assertEquals(listOf(expected), underTest.getDomains(name = domain3.name))
    }

    @Test
    fun `getDomains should return an empty list where no matching names for the specified name exist`() {
        val domains = listOf(domain1, domain2, domain3)
        domains.forEach { underTest.createDomain(it) }
        assertEquals(emptyList<Domain>(), underTest.getDomains(name = "This domain does not exist"))
    }

    @Test
    fun `deleteDomain should delete the specified domain where it exists`() {
        val domains = listOf(domain1, domain2, domain3)
        domains.forEach { underTest.createDomain(it) }
        assertDoesNotThrow { underTest.deleteDomain(domain1.id) }
        assertNull(underTest.getDomain(domain1.id))
    }

    @Test
    fun `deleteDomain should throw a DeleteFailedException if the specified id does not exist`() {
        assertThrows(DeleteFailedException::class.java) { underTest.deleteDomain(UUID.randomUUID()) }
    }

    @Test
    fun `updateDomain should update an existing Domain`() {
        val domains = listOf(domain1, domain2, domain3)
        domains.forEach { underTest.createDomain(it) }

        val updatedDomain = underTest.getDomain(domain1.id)?.copy(
            description = "This is an updated description for the domain",
            status = Status.PUBLISHED,
        )!!

        assertDoesNotThrow { underTest.updateDomain(updatedDomain) }
        assertEquals(updatedDomain.setTimestamps(fixedClock.instant()), underTest.getDomain(domain1.id))

        // Verify that the other domains are unaffected by the update.
        listOf(domain2, domain3).forEach {
            assertEquals(it.setTimestamps(fixedClock.instant()), underTest.getDomain(it.id))
        }

        // As a final check, verify that we can insert another domain with the same name, but with the draft state.
        assertDoesNotThrow { underTest.createDomain(domain1.copy(id = UUID.randomUUID())) }
    }

    @Test
    fun `updateDomain should throw an UpdateFailedException if there is no domain record to update`() {
        val updatedDomain = domain1.copy(
            description = "This is an updated description for the domain"
        )
        assertThrows(UpdateFailedException::class.java) { underTest.updateDomain(updatedDomain) }
        assertNull( underTest.getDomain(domain1.id))
    }

    private fun Domain.setTimestamps(ts: Instant): Domain {
        return this.copy(created = ts, lastUpdated = ts)
    }

}