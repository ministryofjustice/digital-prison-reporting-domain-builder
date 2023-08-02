package uk.gov.justice.digital.test

import uk.gov.justice.digital.model.*
import java.util.*

object Fixtures {

    val TEST_API_KEY = "test-api-key"

    val EMAIL = "someone@example.com"

    val tags = mapOf(
        Pair("department", "ABCDEFG"),
        Pair("unit", "Somewhere"),
    )

    val transform = Transform(
        viewText = "SELECT source.table.field1, source.table.field2 FROM source.table",
        sources = listOf("source.table"),
    )

    val mapping = Mapping(
        viewText = "SELECT another.table.field FROM another.table"
    )

    val violation = Violation(
        check = "Some violation check",
        location = "some/location",
        name = "Some violation name"
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
        mapping = mapping,
        violations = listOf(violation),
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
        status = Status.DRAFT,
        description = "A domain",
        version = "0.0.1",
        location = "/domain1",
        tags = tags,
        owner = EMAIL,
        author = EMAIL,
        tables = listOf(table1),
    )

    val publishedDomain1 = domain1.copy(
        id = UUID.randomUUID(),
        status = Status.PUBLISHED
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

    val domains = listOf(domain1, domain2, domain3)

    val writeableDomain = WriteableDomain(
        name = "New Domain 1",
        status = Status.DRAFT,
        description = "A new domain",
        version = "0.0.1",
        location = "/newDomain1",
        tags = tags,
        owner = EMAIL,
        author = EMAIL,
        tables = listOf(table1),
    )

    val writeableDomainWithInvalidMappingSql = writeableDomain.copy(
        tables = writeableDomain.tables?.map {
            it.copy(mapping = mapping.copy(viewText = "This is not valid SQL"))
        }
    )

    val writeableDomainWithInvalidTransformSql = writeableDomain.copy(
        tables = writeableDomain.tables?.map {
            it.copy(transform = transform.copy(viewText = "This is not valid SQL"))
        }
    )

    val domainPreviewData: List<List<String?>> =
        listOf(
            listOf("foo", "bar", "baz"),
            listOf("1", null, "1")
        )

}