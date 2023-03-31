package uk.gov.justice.digital

import io.micronaut.configuration.picocli.PicocliRunner
import jakarta.inject.Singleton
import picocli.CommandLine.Command
import uk.gov.justice.digital.command.Domain

@Command(
    name = "domain-builder",
    mixinStandardHelpOptions = true,
    subcommands = [Domain::class]
)
@Singleton
class DomainBuilder : Runnable {

    override fun run() {
        println("run called")
    }

}

fun main(args: Array<String>) {
    PicocliRunner.run(DomainBuilder::class.java, *args);
}