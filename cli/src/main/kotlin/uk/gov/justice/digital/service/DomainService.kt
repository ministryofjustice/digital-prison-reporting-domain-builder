package uk.gov.justice.digital.service

import jakarta.inject.Singleton
import uk.gov.justice.digital.model.Domain
import uk.gov.justice.digital.model.Table
import uk.gov.justice.digital.model.Transform
import uk.gov.justice.digital.service.StaticData.domain1
import uk.gov.justice.digital.service.StaticData.domain2
import uk.gov.justice.digital.service.StaticData.domain3
import java.util.*

/**
 * Initial service that will handle calls to the backend API and processing the results.
 *
 * For now this just provides hardcoded data to support the development of the CLI.
 *
 * See DPR-363 which covers the work to integrate the CLI with the backend API.
 */
@Singleton
class DomainService {

    private val domains = listOf(
        domain1,
        domain2,
        domain3,
        domain1,
        domain2,
        domain3,
        domain1,
        domain2,
        domain3,
        domain1,
        domain2,
        domain3,
        domain1,
        domain2,
        domain3,
        domain1,
        domain2,
        domain3,
        domain1,
        domain2,
        domain3,
        domain1,
        domain2,
        domain3,
        domain1,
        domain2,
        domain3,
        domain1,
        domain2,
        domain3,
        domain1,
        domain2,
        domain3,
        domain1,
        domain2,
        domain3,
        domain1,
        domain2,
        domain3,
    )

    fun getAllDomains(): List<Domain> {
        return domains
    }

    fun getDomainWithName(name: String): Domain? {
        return domains.firstOrNull { it.name == name }
    }

}

// Temporary hardcoded data
object StaticData {

    val EMAIL = "someone@example.com"

    val tags = mapOf(
        Pair("department", "ABCDEFG"),
        Pair("unit", "Somewhere"),
    )

    val transform = Transform(
        viewText = "SELECT source.table.field1, source.table.field2 FROM source.table",
        sources = listOf("source.table"),
    )

    val table1 = Table(
        name = "Table 1",
        description = "A table containing some data",
        version = "0.0.1",
        location = "/table1",
        tags = tags,
        owner = EMAIL,
        author = EMAIL,
        primaryKey = "id",
        transform = transform,
        violations = emptyList(),
    )

    val table2 = table1.copy(
        name = "Table 2",
        description = "Another table containing some data",
        location = "/table2",
    )

    val table3 = table1.copy(
        name = "Table 3",
        description = "Yet another table containing some data",
        location = "/table3",
        transform = transform.copy(sources = listOf("source.table", "anotherSource.anotherTable"))
    )

    val domain1 = Domain(
        id = UUID.randomUUID(),
        name = "Domain 1",
        description = "A domain",
        version = "0.0.1",
        location = "/domain1",
        tags = tags,
        owner = EMAIL,
        author = EMAIL,
        originator = EMAIL,
        tables = listOf(table1),
    )

    val domain2 = domain1.copy(
        id = UUID.randomUUID(),
        name = "Domain 2",
        description = "Another domain",
        location = "/domain2",
        tables = listOf(table2, table3)
    )

    val domain3 = domain1.copy(
        id = UUID.randomUUID(),
        name = "Domain 3",
        description = "Yet another domain",
        location = "/domain3",
        tables = listOf(table1, table2, table3)
    )

}