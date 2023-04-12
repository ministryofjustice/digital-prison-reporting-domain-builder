package uk.gov.justice.digital.test

import uk.gov.justice.digital.model.Domain
import uk.gov.justice.digital.model.Table
import uk.gov.justice.digital.model.Transform
import java.util.*

object Fixtures {

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