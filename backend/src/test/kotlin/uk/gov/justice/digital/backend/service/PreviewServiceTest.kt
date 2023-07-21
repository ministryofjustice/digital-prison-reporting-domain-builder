package uk.gov.justice.digital.backend.service

import io.mockk.every
import io.mockk.mockk
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import uk.gov.justice.digital.backend.repository.DomainRepository
import uk.gov.justice.digital.model.Status
import uk.gov.justice.digital.test.Fixtures.domain1

@Testcontainers
class PreviewServiceTest {

    companion object {
        @Container
        val postgresContainer: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:15.2")
            .withDatabaseName("test_domains")
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
                .locations("classpath:testdb/migration")
                .load()
                .migrate()
        }
    }

    private val mockRepository = mockk<DomainRepository>()

    private val underTest by lazy { PreviewService(dataSource, mockRepository) }

    @Test
    fun `it should return the query result as a list of maps`() {
        val domainWithTestDomainQuery = domain1.copy(
            tables = domain1.tables.map {
                it.copy(transform = it.transform.copy(viewText = "select * from test_domain"))
            }
        )

        every { mockRepository.getDomains(any(), any()) } answers { listOf(domainWithTestDomainQuery) }

        val result = underTest.preview("Test Domain", Status.DRAFT, 50)

        assertEquals(5, result.size)

        val expectedFirstRow = mapOf(
            "id" to "1",
            "name" to "Foo",
            "street" to "Montague Road",
            "postcode" to "SW12 2PB",
            "items" to "5"
        )

        assertEquals(expectedFirstRow, result[0])
    }
}