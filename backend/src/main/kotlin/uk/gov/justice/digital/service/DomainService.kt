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
 * Initial service that will handle calls to the backend database.
 *
 * For now this just provides hardcoded data to support the development of the API.
 *
 * Part of DPR-362 - the work to build the REST API endpoints ultimately backed by a DB.
 */
@Singleton
class DomainService {

    private val domains = listOf(
        domain1,
        domain2,
        domain3,
    )

    fun getDomains(
        name: String? = null,
        // TODO - additional filters will be specified in later work - see DPR-333
    ): List<Domain> {
        return domains.filter { domain ->
            // If name is not null, check names for equality otherwise return true.
            // This will allow us to handle multiple optional parameters easily in that null parameters will simply be
            // ignored, and only those predicates with a value specified will be applied.
            name?.let { it == domain.name } ?: true
        }
    }

    fun getDomain(id: UUID): Domain? {
        return domains.firstOrNull { it.id == id }
    }

}

// Temporary hardcoded data - See DPR-363 for work to integrate with backend.
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