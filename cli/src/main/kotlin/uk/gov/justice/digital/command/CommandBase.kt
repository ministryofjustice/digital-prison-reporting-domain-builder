package uk.gov.justice.digital.command

import picocli.CommandLine.Help.Ansi

abstract class CommandBase {

    protected fun printlnAnsi(s: String) {
        println(Ansi.AUTO.string(s))
    }

}