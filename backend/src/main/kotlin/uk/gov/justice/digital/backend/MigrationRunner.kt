package uk.gov.justice.digital.backend

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import io.micronaut.configuration.picocli.PicocliRunner
import jakarta.inject.Singleton
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import picocli.CommandLine.Command
import javax.sql.DataSource
import kotlin.system.exitProcess

/**
 * Command to apply migrations on the configured database.
 *
 * This can be used to apply migrations locally or as part of a deployment pipeline as an AWS Lambda function.
 *
 * See script ./bin/apply-migrations which executes this command class.
 */
@Singleton
@Command(name = "MigrationRunner")
class MigrationRunner(private val defaultDataSource: DataSource) : Runnable, RequestHandler<Unit, Unit> {

    private val logger = LoggerFactory.getLogger(this::class.java)

    // Run method for conventional execution via main method with output sent to STDOUT.
    override fun run() {
        println("Applying migrations")
        runMigrationsWithExceptionHandler { e ->
            println("Migrations failed to apply")
            println("Error: $e")
            println("Stacktrace:")
            e.printStackTrace()
            exitProcess(1)
        }
        println("Migrations applied successfully")
    }

    // AWS Lambda request handler for execution on AWS as a Lambda Function.
    override fun handleRequest(input: Unit?, context: Context?) {
        logger.info("Applying migrations")
        runMigrationsWithExceptionHandler { e ->
            logger.error("Failed to apply migrations", e)
            throw e
        }
        logger.info("Migrations applied successfully")
    }

    private fun runMigrationsWithExceptionHandler(handleException: (Exception) -> Unit) {
        try {
            Flyway
                .configure()
                .dataSource(defaultDataSource)
                .load()
                .migrate()
        } catch (e: Exception) { handleException(e) }
    }


    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            PicocliRunner.run(MigrationRunner::class.java)
        }
    }

}