package uk.gov.justice.digital.backend.repository.table

import org.ktorm.jackson.json
import org.ktorm.schema.*
import uk.gov.justice.digital.model.Domain
import uk.gov.justice.digital.model.Status

object DomainTable : Table<Nothing>("domain") {
    val id = uuid("id").primaryKey()
    val name = varchar("name")
    val status = enum<Status>("status")
    val data = json<Domain>("data")
    val created = timestamp("created")
    val lastUpdated = timestamp("lastupdated")
}