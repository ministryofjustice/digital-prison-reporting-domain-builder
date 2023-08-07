package uk.gov.justice.digital.backend.client.domain

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import jakarta.inject.Singleton
import java.util.*

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

}