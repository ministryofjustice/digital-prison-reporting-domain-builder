package uk.gov.justice.digital.backend.client.preview

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
class AthenaPreviewClientTest {

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

    private val underTest by lazy { AthenaPreviewClient(dataSource) }

    @Test
    fun `it should execute the query and return the results for valid sql`() {

        val result = underTest.runQuery("select * from test_domain")

        assertEquals(6, result.size)

        val headingsRow = listOf(
            "id",
            "name",
            "street",
            "postcode",
            "items"
        )

        val firstDataRow = listOf(
            "1",
            "Foo",
            "Montague Road",
            "SW12 2PB",
            "5"
        )

        // First row of results contains headings...
        assertEquals(headingsRow, result[0])
        // ...followed by data.
        assertEquals(firstDataRow, result[1])

    }

}