package uk.gov.justice.digital.command

import org.jline.builtins.Less
import org.jline.builtins.Source
import org.jline.terminal.Terminal
import picocli.CommandLine.Help.Ansi
import java.io.ByteArrayInputStream

abstract class CommandBase {

    private val pagerText = "use arrow keys to move up and down | press q to exit this view | press h for help"

    protected fun printAnsi(terminal: Terminal, s: String) {
        terminal.writer().println(Ansi.AUTO.string(s))
    }

    protected fun pagedAnsi(terminal: Terminal, s: String) {
        val ansiOutput = Ansi.AUTO.string(s)
        val src = Source.InputStreamSource(ByteArrayInputStream(ansiOutput.toByteArray()), true, pagerText)
        val less = Less(terminal, null)
        // Do not page if the output fits on the screen
        less.quitIfOneScreen = true
        less.run(src)
    }

}