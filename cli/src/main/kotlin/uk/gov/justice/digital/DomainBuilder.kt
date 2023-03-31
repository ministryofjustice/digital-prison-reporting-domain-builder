package uk.gov.justice.digital

import io.micronaut.configuration.picocli.PicocliRunner
import jakarta.inject.Singleton
import picocli.CommandLine.Command
import uk.gov.justice.digital.command.Domain

@Command(
    name = "domain-builder",
    mixinStandardHelpOptions = true,
    version = ["domain-builder 0.0.1"],
    subcommands = [Domain::class]
)
@Singleton
class DomainBuilder : Runnable {

    override fun run() {}

}

fun main(args: Array<String>) {
    PicocliRunner.run(DomainBuilder::class.java, *args);
}