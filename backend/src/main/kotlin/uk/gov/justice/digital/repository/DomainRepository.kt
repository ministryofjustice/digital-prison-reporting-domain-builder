package uk.gov.justice.digital.repository

import jakarta.inject.Singleton
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.dsl.select
import org.ktorm.dsl.where
import uk.gov.justice.digital.model.Domain
import uk.gov.justice.digital.repository.table.DomainTable
import java.time.Instant
import java.util.*
import javax.sql.DataSource

@Singleton
class DomainRepository(private val dataSource: DataSource) {

    private val database = Database.connect(dataSource)

    // TODO - instead of returning ints, return unit and throw on 0?

    fun getDomain(id: UUID): Domain?  {
        return database
            .from(DomainTable)
            .select(DomainTable.data)
            .where(DomainTable.id eq id)
            .map { it[DomainTable.data] }
            .firstOrNull()
    }

    fun getDomains(name: String? = null): List<Domain> {
        return database
            .from(DomainTable)
            .select(DomainTable.data)
            .whereWithConditions { conditions ->
                name?.let { conditions += DomainTable.name eq it }
            }
            .map { it[DomainTable.data] }
            .filterNotNull()
    }

    fun createDomain(domain: Domain): Int {
        val timestamp = Instant.now()
        return database
            .insert(DomainTable) {
                set(it.id, domain.id)
                set(it.name, domain.name)
                set(it.data, domain)
                set(it.created, timestamp)
                set(it.lastUpdated, timestamp)
        }
    }

    fun updateDomain(domain: Domain): Int {
        return database
            .update(DomainTable) {
                set(it.id, domain.id)
                set(it.name, domain.name)
                set(it.data, domain)
                set(it.lastUpdated, Instant.now())
            }

    }

    fun deleteDomain(id: UUID): Int {
        return database.delete(DomainTable) { it.id eq id }
    }

}