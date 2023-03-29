package uk.gov.justice.digital

import jakarta.inject.Singleton
import picocli.CommandLine.Command

// TODO - review picolocli options to determine if separate entry points are needed for CLI and interactive modes

@Singleton
@Command(name = "CliApp")
class CliApp : Runnable {

    override fun run() {
        println("run called")
    }

}

fun main(args: Array<String>) {
    println("Got args: '${args.joinToString(", ")}'")
    println("Calling run");
    val command = CliApp()
    command.run()
}