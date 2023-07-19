package uk.gov.justice.digital.backend.service

import io.burt.athena.AthenaDriver
import jakarta.inject.Named
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.sql.Connection
import javax.sql.DataSource

@Singleton
class PreviewService(@Named("preview") private val dataSource: DataSource) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun preview(sql: String): List<Map<String, String>> {
        logger.info("Executing query: {}", sql)
        val startTime = System.currentTimeMillis()
        val statement = dataSource.configuredConnection().createStatement()
        val resultSet = statement.executeQuery(sql)
        val duration = System.currentTimeMillis() - startTime
        logger.info("Query executed successfully in {}ms", duration)

        val metadata = resultSet.metaData
        val columns = metadata.columnCount

        logger.info("Result set has {} columns", columns)

        // Column names start from 1
        val columnNames = (1..columns)
            .map { metadata.getColumnName(it) }

        logger.info("Result set has the column names: {}", columnNames)

        val result = mutableListOf<Map<String, String>>()

        while(resultSet.next()) {
            val thing = resultSet.getString("create_datetime")
            logger.info("got thing: {}", thing)

            val row = columnNames.associateWith {
                logger.info("Processing column: {}", it)
                val value = resultSet.getObject(it)?.toString() ?: ""
                logger.info("Column has value: {}", value)
                value
            }
            result.add(row)
        }

        return result
    }

    // The athena driver requires some additional properties to be set since they are not parsed from the URL or data
    // sources configuration.
    private fun DataSource.configuredConnection(): Connection {
        val connection = this.connection
        connection.setClientInfo(AthenaDriver.REGION_PROPERTY_NAME, "eu-west-2")
        connection.setClientInfo(AthenaDriver.WORK_GROUP_PROPERTY_NAME, "primary")
        connection.setClientInfo(AthenaDriver.OUTPUT_LOCATION_PROPERTY_NAME, "s3://dpr-357-athena-test")
        return connection
    }

}