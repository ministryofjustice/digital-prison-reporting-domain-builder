package uk.gov.justice.digital.backend.service

import jakarta.inject.Named
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import javax.sql.DataSource

@Singleton
class PreviewService(@Named("preview") private val dataSource: DataSource) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun preview(sql: String): List<Map<String, String>> {
        logger.info("Executing query: {}", sql)
        val startTime = System.currentTimeMillis()
        val statement = dataSource.connection.createStatement()
        val resultSet = statement.executeQuery(sql)
        val duration = System.currentTimeMillis() - startTime
        logger.info("Query executed successfully in {}ms", duration)

        val metadata = resultSet.metaData
        val columns = metadata.columnCount

        // Column names start from 1
        val columnNames = (1..columns)
            .map { metadata.getColumnName(it) }

        val result = mutableListOf<Map<String, String>>()

        while(resultSet.next()) {
            result.add( columnNames.associateWith { resultSet.getString(it) } )
        }

        return result
    }

}