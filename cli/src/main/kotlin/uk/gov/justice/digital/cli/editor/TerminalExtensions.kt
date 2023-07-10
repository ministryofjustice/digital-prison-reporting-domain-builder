package uk.gov.justice.digital.cli.editor

import org.jline.terminal.Terminal
import org.jline.utils.InfoCmp.Capability

fun Terminal.hideCursor() = this.puts(Capability.cursor_invisible)
fun Terminal.showCursor() = this.puts(Capability.cursor_visible)

fun Terminal.moveCursorToHome() {
    this.puts(Capability.cursor_home)
    this.flush()
}

fun Terminal.moveCursorUp(lines: Int) = applyAction(Capability.cursor_up, lines)
fun Terminal.moveCursorDown(lines: Int) = applyAction(Capability.cursor_down, lines)
fun Terminal.moveCursorLeft(columns: Int) = applyAction(Capability.cursor_left, columns)
fun Terminal.moveCursorRight(columns: Int) = applyAction(Capability.cursor_right, columns)

fun Terminal.applyAction(action: Capability, count: Int = 1) {
    for (i in 1..count) { this.puts(action) }
    this.flush()
}

fun Terminal.moveCursorTo(line: Int, column: Int) {
    print("\u001B[$line;${column}H")
    this.flush()
}

fun Terminal.clearDisplay() {
    this.puts(Capability.clear_screen)
    this.flush()
}

fun Terminal.clearLine() {
    print("\u001B[2K")
    this.flush()
}

fun Terminal.bell() {
    this.puts(Capability.bell)
//    terminal.flush()
}

fun Terminal.saveCursorPosition() = print("\u001B7")
fun Terminal.restoreSavedCursorPosition() = print("\u001B8")
