package uk.gov.justice.digital

import io.micronaut.configuration.picocli.MicronautFactory
import io.micronaut.configuration.picocli.PicocliRunner
import jakarta.inject.Singleton
import org.fusesource.jansi.AnsiConsole
import org.jline.console.SystemRegistry
import org.jline.console.impl.SystemRegistryImpl
import org.jline.reader.*
import org.jline.reader.impl.DefaultParser
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.jline.widget.AutosuggestionWidgets
import picocli.CommandLine
import picocli.CommandLine.*
import picocli.shell.jline3.PicocliCommands
import uk.gov.justice.digital.command.CommandBase
import uk.gov.justice.digital.command.ListDomains
import uk.gov.justice.digital.command.ViewDomain
import java.io.PrintWriter

@Command(
    name = "domain-builder",
    subcommands = [
        HelpCommand::class,
        ListDomains::class,
        ViewDomain::class,
    ],
)
@Singleton
class DomainBuilder : CommandBase(), Runnable {

    @Option(
        names = ["-i", "--interactive"],
        description = ["Run the command in interactive mode"],
        required = false
    )
    var interactive = false

    @Option(
        names = ["-h", "--help"],
        usageHelp = true,
        description = [ "display this help message" ]
    )
    var usageHelpRequested = false

    private var out: PrintWriter? = null

    private fun setReader(reader: LineReader) {
        out = reader.terminal.writer()
    }

    private val launchText = """
        
        @|bold,cyan  ____                                  ____          _     _               |@
        @|bold,cyan |  _ \  ___  _ __ ___   __ _(_)_ __   | __ ) _   _(_) | __| | ___ _ __     |@
        @|bold,cyan | | | |/ _ \| '_ ` _ \ / _` | | '_ \  |  _ \| | | | | |/ _` |/ _ \ '__|    |@
        @|bold,cyan | |_| | (_) | | | | | | (_| | | | | | | |_) | |_| | | | (_| |  __/ |       |@
        @|bold,cyan |____/ \___/|_| |_| |_|\__,_|_|_| |_| |____/ \__,_|_|_|\__,_|\___|_|       |@

        Type @|bold help|@ to view available commands.
        
        Type @|bold <command> --help|@ to view help for a specific command.
        
        Type @|bold exit|@ to exit the domain builder.
        
        Press the @|bold TAB|@ key to view available commands or autocomplete commands as you type.
        
    """.trimIndent()

    override fun run() {
        if (interactive) {
            printlnAnsi(launchText)

            AnsiConsole.systemInstall()

            try {
                TerminalBuilder.builder().build().use { terminal -> interactiveSession(terminal) }
            } catch (t: Throwable) {
                t.printStackTrace()
            } finally {
                AnsiConsole.systemUninstall()
            }
        }
    }

    private fun interactiveSession(terminal: Terminal) {
        // Set up picocli commands
        val commands = this
        val factory = MicronautFactory()
        val cmd = CommandLine(commands, factory)
        val picocliCommands = PicocliCommands(cmd)
        val parser: Parser = DefaultParser()
        val systemRegistry: SystemRegistry = SystemRegistryImpl(parser, terminal, null, null)

        systemRegistry.setCommandRegistries(picocliCommands)
        systemRegistry.register("help", picocliCommands)

        val reader = LineReaderBuilder.builder()
            .terminal(terminal)
            .completer(systemRegistry.completer())
            .parser(parser)
            .variable(LineReader.LIST_MAX, 50) // max tab completion candidates
            .build()
        commands.setReader(reader)

        AutosuggestionWidgets(reader).enable()

        val prompt = "domain-builder> "

        // Start the shell and process input until the user quits with Ctrl-D
        while (true) {
            try {
                systemRegistry.cleanUp()
                systemRegistry.execute(reader.readLine(prompt, null, null))
            } catch (e: UserInterruptException) {
                // Ignore
            } catch (e: EndOfFileException) {
                return
            } catch (e: Exception) {
                systemRegistry.trace(e)
            }
        }

    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            System.setProperty("picocli.ansi", "true")
            PicocliRunner.execute(DomainBuilder::class.java, *args)
        }
    }

}
