package uk.gov.justice.digital.backend.service

import jakarta.inject.Singleton
import uk.gov.justice.digital.backend.client.preview.PreviewClient
import uk.gov.justice.digital.backend.converter.DomainToPreviewQueryConverter
import uk.gov.justice.digital.backend.repository.DomainRepository
import uk.gov.justice.digital.model.Status
import kotlin.math.min

@Singleton
class AthenaPreviewService(private val client: PreviewClient,
                           private val repository: DomainRepository,
                           private val converter: DomainToPreviewQueryConverter): PreviewService {

    override fun preview(domainName: String, status: Status, limit: Int): List<List<String>> {
        val domains = repository.getDomains(domainName, status)

        val domain = domains.firstOrNull()
            ?: throw DomainNotFoundException("No domains found for name: $domainName status: $status")

        // We expect a domain definition to contain a single table.
        return domain.tables
            ?.firstOrNull()
            ?.let { preview(it.transform.viewText, limit )}
            ?: throw NoTablesInDomainException("No tables found in domain with name: $domainName status: $status")
    }

    private fun preview(sql: String, limit: Int): List<List<String>> {
        val convertedQuery = converter.convertQuery(sql, min(limit, PreviewService.MaximumLimit))
        return client.runQuery(convertedQuery)
    }

}

sealed class PreviewServiceException(message: String, cause: Exception? = null) : RuntimeException(message, cause)
class DomainNotFoundException(message: String) : PreviewServiceException(message)
class NoTablesInDomainException(message: String) : PreviewServiceException(message)
