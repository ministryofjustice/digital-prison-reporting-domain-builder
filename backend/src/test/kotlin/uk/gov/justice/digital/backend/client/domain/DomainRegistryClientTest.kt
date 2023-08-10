package uk.gov.justice.digital.backend.client.domain

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.QueryResult
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsResult
import io.micronaut.serde.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.backend.client.domain.DomainRegistryClient.Companion.Fields
import uk.gov.justice.digital.model.Status
import uk.gov.justice.digital.test.Fixtures.domain1
import java.util.*

class DomainRegistryClientTest {

    private val mockProvider: DynamoDBClientProvider = mockk()
    private val mockClient: AmazonDynamoDB = mockk()
    private val mockObjectMapper: ObjectMapper = mockk()

    private val domainRegistryName = "test-registry"

    private val underTest = DomainRegistryClient(mockProvider, domainRegistryName, mockObjectMapper)

    @Test
    fun `publish should delete an existing domain and replace it with the provided domain`() {
        // Set up mocks and any fake client responses.
        val fakeQueryResponse = QueryResult().withItems(
            mapOf(
                Fields.PRIMARY_ID to AttributeValue().withS(UUID.randomUUID().toString()),
                Fields.SECONDARY_ID to AttributeValue().withS("test-domain"),
                Fields.TYPE to AttributeValue().withS("domain"),
                Fields.DATA to AttributeValue().withS("""
                    { "foo": "${UUID.randomUUID()} }"
                """.trimIndent())
            )
        )

        val publishedDomain = domain1.copy(status = Status.PUBLISHED)

        every { mockProvider.client } returns mockClient
        every { mockClient.query(any()) } returns fakeQueryResponse
        every { mockClient.transactWriteItems(any()) } returns TransactWriteItemsResult()
        every { mockObjectMapper.writeValueAsString(publishedDomain) } returns """{ "foo": "bar" }"""

        underTest.publish(publishedDomain)

        verify { mockClient.query(any()) }
        verify { mockClient.transactWriteItems(any()) }
    }

}