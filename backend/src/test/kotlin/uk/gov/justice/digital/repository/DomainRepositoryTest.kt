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
import uk.gov.justice.digital.test.Fixtures.domain1
import uk.gov.justice.digital.test.Fixtures.domain2
import uk.gov.justice.digital.test.Fixtures.domain3
import java.util.*

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

    private val underTest by lazy { DomainRepository(dataSource) }

    @Test
    fun `createDomain should succeed for a successful insert`() {
        assertDoesNotThrow { underTest.createDomain(domain1) }
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
    fun `getDomain should return a Domain where a domain for the UUID exists`() {
        underTest.createDomain(domain1)
        assertEquals(domain1, underTest.getDomain(domain1.id))
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
        assertEquals(domains, underTest.getDomains())
    }

    @Test
    fun `getDomains should return a single domain where a name is specified which matches an existing Domain`() {
        val domains = listOf(domain1, domain2, domain3)
        domains.forEach { underTest.createDomain(it) }
        assertEquals(listOf(domain3), underTest.getDomains(name = domain3.name))
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

        val updatedDomain = domain1.copy(
            description = "This is an updated description for the domain"
        )

        assertDoesNotThrow { underTest.updateDomain(updatedDomain) }
        assertEquals(updatedDomain, underTest.getDomain(domain1.id))
        // Verify that the other domains are unaffected by the update.
        listOf(domain2, domain3).forEach {
            assertEquals(it, underTest.getDomain(it.id))
        }
    }

    @Test
    fun `updateDomain should throw an UpdateFailedException if there is no domain record to update`() {
        val updatedDomain = domain1.copy(
            description = "This is an updated description for the domain"
        )
        assertThrows(UpdateFailedException::class.java) { underTest.updateDomain(updatedDomain) }
        assertNull( underTest.getDomain(domain1.id))
    }

}