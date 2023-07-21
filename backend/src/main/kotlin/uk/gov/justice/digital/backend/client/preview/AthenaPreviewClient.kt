package uk.gov.justice.digital.backend.client.preview

import jakarta.inject.Named
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import javax.sql.DataSource

@Singleton
class AthenaPreviewClient(@Named("preview") private val previewDataSource: DataSource) : PreviewClient {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun runQuery(query: String): List<Map<String, String>> {
        logger.info("Executing query: {}", query)
        val startTime = System.currentTimeMillis()
        val statement = previewDataSource.connection.createStatement()
        val resultSet = statement.executeQuery(query)
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