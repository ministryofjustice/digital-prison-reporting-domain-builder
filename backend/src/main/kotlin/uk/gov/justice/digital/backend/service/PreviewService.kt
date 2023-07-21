package uk.gov.justice.digital.backend.service

import jakarta.inject.Named
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import uk.gov.justice.digital.backend.repository.DomainRepository
import uk.gov.justice.digital.model.Status
import javax.sql.DataSource

@Singleton
// TODO - wrap the data source to provide a simpler API
// TODO - impose upper bound on limit
class PreviewService(@Named("preview") private val previewDataSource: DataSource,
                     private val repository: DomainRepository) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun preview(domainName: String, status: Status?, limit: Int): List<Map<String, String>> {
        val domains = repository.getDomains(domainName, status)

        val domain = if (domains.isEmpty())
            throw DomainNotFoundException("No domains found for name: $domainName status: $status")
        else if (domains.size > 1)
            throw MultipleDomainsFoundException("More than one matching domain for name: $domainName status: $status")
        else domains.first()

        // TODO - right now we only handle single table domains so we just preview the first table where present.
        return domain.tables
            .firstOrNull()
            ?.let { preview(it.transform.viewText, limit )}
            ?: throw NoTablesInDomainException("No tables found in domain with name: $domainName stauts: $status")
    }

    // TODO - this should be moved into another class
    private fun preview(sql: String, limit: Int): List<Map<String, String>> {
        // TODO - add limit clause to query string
        logger.info("Executing query: {}", sql)
        val startTime = System.currentTimeMillis()
        val statement = previewDataSource.connection.createStatement()
        val resultSet = statement.executeQuery(sql)
        val duration = System.currentTimeMillis() - startTime
        logger.info("Query executed successfully in {}ms", duration)

        val metadata = resultSet.metaData
        val columnCount = metadata.columnCount

        // Column names start from 1
        val columnNames = (1..columnCount)
            .map { metadata.getColumnName(it) }

        val result = mutableListOf<Map<String, String>>()

        while(resultSet.next()) {
            result.add( columnNames.associateWith { resultSet.getString(it) } )
        }

        return result
    }

}

sealed class PreviewServiceException(message: String, cause: Exception? = null) : RuntimeException(message, cause)
class DomainNotFoundException(message: String) : PreviewServiceException(message)
class MultipleDomainsFoundException(message: String) : PreviewServiceException(message)
class NoTablesInDomainException(message: String) : PreviewServiceException(message)