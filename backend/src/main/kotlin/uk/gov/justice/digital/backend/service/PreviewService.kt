package uk.gov.justice.digital.backend.service

import jakarta.inject.Named
import jakarta.inject.Singleton
import javax.sql.DataSource

@Singleton
class PreviewService(@Named("preview") private val dataSource: DataSource) {

    fun preview(sql: String): List<Map<String, String>> {
        val statement = dataSource.connection.createStatement()
        val resultSet = statement.executeQuery(sql)
        val metadata = resultSet.metaData
        val columns = metadata.columnCount

        // Column names start from 1
        val columnNames = (1..columns)
            .map { metadata.getColumnName(it) }

        val result = mutableListOf<Map<String, String>>()

        while(resultSet.next()) {
            val row = columnNames.associateWith { resultSet.getString(it) }
            result.add(row)
        }

        return result
    }

}