package uk.gov.justice.digital

import io.micronaut.configuration.picocli.PicocliRunner
import jakarta.inject.Singleton
import org.flywaydb.core.Flyway
import picocli.CommandLine.Command
import javax.sql.DataSource
import kotlin.system.exitProcess

/**
 * Command to apply migrations on the configured database.
 *
 * This can be used to apply migrations locally or as part of a deployment pipeline.
 *
 * See script ./bin/apply-migrations which executes this command class.
 */
@Singleton
@Command(name = "MigrationRunner")
class MigrationRunner(private val defaultDataSource: DataSource) : Runnable {

    override fun run() {
        println("Applying migrations")
        try {
            Flyway
                .configure()
                .dataSource(defaultDataSource)
                .load()
                .migrate()
        } catch (e: Exception) {
            println("Migrations failed to apply")
            println("Error: $e")
            println("Stacktrace:")
            e.printStackTrace()
            exitProcess(1)
        }
        println("Migrations applied successfully")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            PicocliRunner.run(MigrationRunner::class.java)
        }
    }

}