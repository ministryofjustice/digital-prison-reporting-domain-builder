package uk.gov.justice.digital.backend.client.domain

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.GetItemRequest
import jakarta.inject.Singleton
import uk.gov.justice.digital.model.Domain

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

        // First get the domain by name
        val doaminName = d.name



    }

    // TODO - return
    fun getDomainByName(domainName: String) {

        val request = GetItemRequest(
            "domainName",
        )
        val result = client.getItem()
        client.getItem(
            "domain",
            mapOf(

            )
        )

    }

}