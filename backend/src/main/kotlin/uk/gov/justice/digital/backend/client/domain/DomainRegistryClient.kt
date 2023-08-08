package uk.gov.justice.digital.backend.client.domain

import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.model.*
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator.EQ
import jakarta.inject.Singleton
import uk.gov.justice.digital.model.Domain
import java.util.UUID

/**
 * TODO
 *  o figure out what this needs to do
 *  o will need to update an existing domain with the new one
 *  o may need to delete and insert - specifically where uuids are different which should normally be the case
 */

// TODO - this could also be a repository. Review
@Singleton
class DomainRegistryClient(clientProvider: DynamoDBClientProvider) {

    private val client: AmazonDynamoDB by lazy { clientProvider.client }

    // TODO - determine what the publish process is and what operations are supported by the API
    fun publish(d: Domain) {

        // TODO -  can we do an in-place update

    }

}

// Evolve an approach to replacing an existing domain without causing disruption to any services querying the registry.
object DynTest {

    val client: AmazonDynamoDB by lazy {
        AmazonDynamoDBClientBuilder.standard()
            .withEndpointConfiguration(AwsClientBuilder.EndpointConfiguration(
                "https://dynamodb.eu-west-2.amazonaws.com",
                "eu-west-2"
            ))
            .build()
    }

    @JvmStatic
    fun main(args: Array<String>) {
        println("Attempting domain update")

        val tableName = "dpr-domain-registry-development"
        val domainName = "Domain 114"

        val queryRequest = QueryRequest()
            .withTableName(tableName)
            .withIndexName("secondaryId-type-index")
            .withKeyConditions(
                mapOf(
                    "secondaryId" to Condition()
                        .withAttributeValueList(AttributeValue().withS(domainName))
                        .withComparisonOperator(EQ),
                    "type" to Condition()
                        .withAttributeValueList(AttributeValue().withS("domain"))
                        .withComparisonOperator(EQ),
                )
            )

        val r2 = client.query(queryRequest)

        val deleteWriteItems = r2.items.map {
            TransactWriteItem().withDelete(
                Delete()
                    .withTableName(tableName)
                    .withKey(it.filterKeys { key -> listOf("primaryId", "secondaryId").contains(key) })
                )
        }

        println("Query returned: ${r2.count} results")

        val existingDomain = r2.items[0]

        val existingDomainId = existingDomain["primaryId"]?.s

        println("Existing domain has ID: $existingDomainId")

        val putWriteItem = TransactWriteItem()
            .withPut(
                Put()
                    .withTableName(tableName)
                    .withItem(
                        mapOf(
                            "primaryId" to AttributeValue().withS(UUID.randomUUID().toString()),
                            "secondaryId" to AttributeValue().withS(domainName),
                            "type" to AttributeValue().withS("domain"),
                            "data" to AttributeValue().withS("""
                                { "foo": "${UUID.randomUUID()} }"
                            """.trimIndent())
                        )
                    )
        )

        val result = client.transactWriteItems(TransactWriteItemsRequest()
            .withTransactItems(deleteWriteItems + putWriteItem))

        println("Got reuslt: $result")
    }
}