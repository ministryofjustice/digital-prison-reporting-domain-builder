package uk.gov.justice.digital.command

import picocli.CommandLine.Help.Ansi
import java.io.PrintWriter

abstract class CommandBase {

    protected abstract fun getPrintWriter(): PrintWriter

    protected fun printAnsi(s: String) {
        getPrintWriter().println(Ansi.AUTO.string(s))
    }

}