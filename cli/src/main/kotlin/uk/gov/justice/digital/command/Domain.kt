package uk.gov.justice.digital.command

import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Model.CommandSpec
import picocli.CommandLine.ParameterException
import picocli.CommandLine.Spec
import uk.gov.justice.digital.command.domain.ListDomains
import kotlin.system.exitProcess

@Command(name = "domain", subcommands = [ListDomains::class] )
class Domain : Runnable{

    @Spec
    var spec: CommandSpec? = null

    override fun run() {
        throw ParameterException(spec?.commandLine(), "Specify subcommand")
    }

}

fun main(args: Array<String>) {
    val exitCode = CommandLine(Domain()).execute(*args)
    exitProcess(exitCode)
}