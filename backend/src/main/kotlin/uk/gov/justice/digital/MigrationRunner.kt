package uk.gov.justice.digital

import io.micronaut.configuration.picocli.PicocliRunner
import io.micronaut.flyway.FlywayConfigurationProperties
import io.micronaut.flyway.FlywayMigrator
import jakarta.inject.Singleton
import picocli.CommandLine.Command
import javax.sql.DataSource

@Singleton
@Command(name = "MigrationRunner")
class MigrationRunner(
    private val flywayMigrator: FlywayMigrator,
    private val flywayConfig: FlywayConfigurationProperties,
    private val defaultDataSource: DataSource,
) : Runnable {

    override fun run() {
        println("Applying migrations")
        flywayMigrator.run(flywayConfig, defaultDataSource)
        println("Migrations applied successfully")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            PicocliRunner.run(MigrationRunner::class.java)
        }
    }

}