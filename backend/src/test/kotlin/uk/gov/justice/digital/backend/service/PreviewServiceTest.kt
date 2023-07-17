package uk.gov.justice.digital.backend.service

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

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

    private val underTest by lazy { PreviewService(dataSource) }

    @Test
    fun `it should return the query result as a list of maps`() {
        val result = underTest.preview("select * from test_domain")

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