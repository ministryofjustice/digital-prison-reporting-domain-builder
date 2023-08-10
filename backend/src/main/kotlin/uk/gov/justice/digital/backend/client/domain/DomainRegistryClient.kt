package uk.gov.justice.digital.backend.client.domain

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.*
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator.EQ
import io.micronaut.context.annotation.Value
import io.micronaut.serde.ObjectMapper
import jakarta.inject.Singleton
import uk.gov.justice.digital.backend.client.domain.DomainRegistryClient.Companion.Fields.DATA
import uk.gov.justice.digital.backend.client.domain.DomainRegistryClient.Companion.Fields.PRIMARY_ID
import uk.gov.justice.digital.backend.client.domain.DomainRegistryClient.Companion.Fields.SECONDARY_ID
import uk.gov.justice.digital.backend.client.domain.DomainRegistryClient.Companion.Fields.TYPE
import uk.gov.justice.digital.backend.client.domain.DomainRegistryClient.Companion.Indexes.SECONDARY_ID_TYPE_INDEX
import uk.gov.justice.digital.backend.client.domain.DomainRegistryClient.Companion.Types.DOMAIN
import uk.gov.justice.digital.model.Domain

@Singleton
class DomainRegistryClient(clientProvider: DynamoDBClientProvider,
                           @Value("\${dpr.domainRegistry}") private val domainRegistryName: String,
                           private val objectMapper: ObjectMapper) {

    private val client: AmazonDynamoDB by lazy { clientProvider.client }

    /**
     * Publishes the specified domain in dynamodb with the following steps
     *   o within a transaction
     *      - delete existing domain record
     *      - put specified domain
     *
     * This ensures that we can replace an existing domain with the specified domain without disrupting any running
     * services that depend on that domain being present.
     */
    fun publish(d: Domain) {
        // Get existing domain records and create deletion items for them.
        val existingDomainEntries = getExistingDomainEntries(d.name)
        val existingDomainDeletions = existingDomainEntries.map {
            TransactWriteItem().withDelete(
                Delete()
                    .withTableName(domainRegistryName)
                    .withKey(it.filterKeys { key -> listOf(PRIMARY_ID, SECONDARY_ID).contains(key) })
            )
        }

        // Create a put item for the domain we are publishing.
        val putWriteItem = TransactWriteItem()
            .withPut(
                Put()
                    .withTableName(domainRegistryName)
                    .withItem(
                        mapOf(
                            PRIMARY_ID to d.id.toString().asAttributeValue(),
                            SECONDARY_ID to d.name.asAttributeValue(),
                            TYPE to DOMAIN.asAttributeValue(),
                            DATA to objectMapper.writeValueAsString(d).asAttributeValue(),
                        )
                    )
            )

        client.transactWriteItems(TransactWriteItemsRequest()
            .withTransactItems(existingDomainDeletions + putWriteItem))
    }

    // We should only ever see a single entry for a given domain name. Supporting > 1 entries ensures that we can clean
    // up any unexpected state as part of the publish process.
    private fun getExistingDomainEntries(domainName: String): List<Map<String, AttributeValue>> {
        val existingDomainQuery = QueryRequest()
            .withTableName(domainRegistryName)
            .withIndexName(SECONDARY_ID_TYPE_INDEX)
            .withKeyConditions(
                mapOf(
                    SECONDARY_ID to Condition()
                        .withAttributeValueList(AttributeValue().withS(domainName))
                        .withComparisonOperator(EQ),
                    TYPE to Condition()
                        .withAttributeValueList(AttributeValue().withS(DOMAIN))
                        .withComparisonOperator(EQ),
                )
            )

        return client
            .query(existingDomainQuery)
            .items
    }

    private fun String.asAttributeValue() = AttributeValue().withS(this)

    companion object {

        object Fields {
            const val PRIMARY_ID = "primaryId"
            const val SECONDARY_ID = "secondaryId"
            const val TYPE = "type"
            const val DATA = "data"
        }

        object Indexes {
            const val PRIMARY_ID_TYPE_INDEX = "primaryId-type-index"
            const val SECONDARY_ID_TYPE_INDEX = "secondaryId-type-index"
        }

        object Types {
            const val DOMAIN = "domain"
            // Not currently used - see DPR-218 for the intent here.
            const val TABLE_SOURCE = "tablesource"
        }

    }

}