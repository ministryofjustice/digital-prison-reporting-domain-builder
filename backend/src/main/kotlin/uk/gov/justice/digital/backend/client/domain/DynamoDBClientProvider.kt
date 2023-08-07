package uk.gov.justice.digital.backend.client.domain

import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton

@Singleton
class DynamoDBClientProvider {

    @Value("\${aws.dynamo.endpointUrl}")
    private lateinit var dynamoEndpointUrl: String

    @Value("\${aws.region}")
    private lateinit var awsRegion: String

    val client: AmazonDynamoDB by lazy {
        AmazonDynamoDBClientBuilder.standard()
            .withEndpointConfiguration(AwsClientBuilder.EndpointConfiguration(
                dynamoEndpointUrl,
                awsRegion
            ))
            .build()
    }

}