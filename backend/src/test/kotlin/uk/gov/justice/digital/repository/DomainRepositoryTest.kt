package uk.gov.justice.digital.repository

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.postgresql.ds.PGSimpleDataSource
import org.postgresql.util.PSQLException
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

    @Container
    val postgresContainer: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:15.2")
        .withDatabaseName("test_domain_registry")
        .withUsername("postgres")
        .withPassword("postgres")
        .waitingFor(Wait.forListeningPort())

    private val dataSource by lazy {
        val d = PGSimpleDataSource()
        d.setUrl(postgresContainer.jdbcUrl)
        d.user = "postgres"
        d.password = "postgres"
        d
    }

    private val underTest by lazy {
        DomainRepository(dataSource)
    }

    @BeforeEach
    fun applyMigrations() {
        Flyway
            .configure()
            .dataSource(dataSource)
            .load()
            .migrate()
    }

    @Test
    fun `createDomain should return 1 for a successful insert`() {
        assertEquals(1, underTest.createDomain(domain1))
    }

    @Test
    fun `createDomain should throw an exception a failed insert`() {
        assertEquals(1, underTest.createDomain(domain1))
        assertThrows(PSQLException::class.java) {
            // This second insert attempt should fail since we're trying to insert a duplicate
            assertEquals(0, underTest.createDomain(domain1))
        }
    }

    @Test
    fun `getDomain should return a Domain where a domain for the UUID exists`() {
        assertEquals(1, underTest.createDomain(domain1))
        assertEquals(domain1, underTest.getDomain(domain1.id))
    }

    @Test
    fun `getDomain should return null where no matching Domains exist`() {
        assertEquals(1, underTest.createDomain(domain1))
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
    fun `deleteDomain should delete the specified domain and return 1 where it exists`() {
        underTest.createDomain(domain1)
        val result = underTest.deleteDomain(domain1.id)
        assertEquals(1, result)
        assertNull(underTest.getDomain(domain1.id))
    }

    @Test
    fun `deleteDomain should return 0 if the specified id does not exist`() {
        val result = underTest.deleteDomain(UUID.randomUUID())
        assertEquals(0, result)
    }

    @Test
    fun `updateDomain should update an existing Domain and return 1`() {
        underTest.createDomain(domain1)
        val updatedDomain = domain1.copy(
            description = "This is an updated description for the domain"
        )
        val result = underTest.updateDomain(updatedDomain)
        assertEquals(1, result)
        assertEquals(updatedDomain, underTest.getDomain(domain1.id))
    }

    @Test
    fun `updateDomain should update an existing Domain`() {
        underTest.createDomain(domain1)
        val updatedDomain = domain1.copy(
            description = "This is an updated description for the domain"
        )
        val result = underTest.updateDomain(updatedDomain)
        assertEquals(1, result)
        assertEquals(updatedDomain, underTest.getDomain(domain1.id))
    }

    @Test
    fun `updateDomain should return zero if there is no matching domain record to update`() {
        val updatedDomain = domain1.copy(
            description = "This is an updated description for the domain"
        )
        val result = underTest.updateDomain(updatedDomain)
        assertEquals(0, result)
        assertNull( underTest.getDomain(domain1.id))
    }

}