package uk.gov.justice.digital.repository.table

import org.ktorm.jackson.json
import org.ktorm.schema.*
import uk.gov.justice.digital.model.Domain

object DomainTable : Table<Nothing>("domain") {
    val id = uuid("id").primaryKey()
    val name = varchar("name")
    val data = json<Domain>("data")
    val created = timestamp("created")
    val lastUpdated = timestamp("lastupdated")
}