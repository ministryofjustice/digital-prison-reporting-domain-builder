package uk.gov.justice.digital.backend.client.preview

import jakarta.inject.Named
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import javax.sql.DataSource

@Singleton
class AthenaPreviewClient(@Named("preview") private val previewDataSource: DataSource) : PreviewClient {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun runQuery(query: String): List<List<String>> {
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

        // First row of result contains the column names
        val result = mutableListOf<List<String>>(columnNames)

        while(resultSet.next()) {
            val row = (1..columnCount)
                .map { resultSet.getString(it) }

            result.add(row)
        }

        logger.info("Query returned {} row{}", result.size, if (result.size != 1) "s" else "")

        return result
    }

}