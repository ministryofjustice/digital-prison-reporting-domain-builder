package uk.gov.justice.digital.cli.editor

import org.jline.keymap.BindingReader
import org.jline.keymap.KeyMap
import org.jline.terminal.Terminal
import org.jline.utils.InfoCmp
import uk.gov.justice.digital.cli.editor.MultilineEditor.Operation.*
import uk.gov.justice.digital.cli.session.ConsoleSession
import kotlin.math.min

class MultilineEditor(private val terminal: Terminal,
                      private val session: ConsoleSession,
                      private val heading: String) {

    fun run(text: String? = null): String {

        val originalAttributes = terminal.enterRawMode()

        terminal.puts(InfoCmp.Capability.cursor_visible)
        terminal.puts(InfoCmp.Capability.clear_screen)
        terminal.flush()

        val padding = " ".repeat(terminal.width - heading.length - 1)
        println()
        println(session.toAnsi("@|fg(black),bg(cyan),bold  $heading$padding|@"))
        println()

        val editReader = BindingReader(terminal.reader())

        val map = bindKeys()


        val minLine = 0
        val maxLine = 20 // TODO - make this dynamic?
        val minColumn = 0
        val maxColumn = terminal.width - 1

        var currentLine = 0
        var currentColumn = 0

        val lines = Array(20) { _ -> ""}.toMutableList()
        // Populate lines with existing data where defined
        text?.let {
            text.split("\n")
                .withIndex()
                .forEach { lines[it.index] = it.value }
        }

        lines.forEach { println(it) }
        repeat(lines.size) { terminal.puts(InfoCmp.Capability.cursor_up) }
        terminal.flush()

        print("\u001B7") // Save cursor pos
        print("\u001B[23;0H")
        val statusLine = "keys │ ↑ move up │ ↓ move down │ ← move left │ → move right │ CTRL-S save and exit │ ESC exit".take(78)
        val statusPadding = " ".repeat(terminal.width - min(statusLine.length, terminal.width) - 1)
        print(session.toAnsi("@|fg(black),bg(white),bold  $statusLine$statusPadding|@"))
        print("\u001B8") // restore saved cursor pos
        terminal.flush()

        while(true) {
            when (editReader.readBinding(map, null, true)) {
                UP -> if (currentLine > minLine) {
                    terminal.puts(InfoCmp.Capability.cursor_up)
                    currentLine--
                    val line = lines[currentLine]

                    if (currentColumn > line.length) {
                        print("\u001B[0G")
                        print("\u001B[${line.length + 1}G")
                        currentColumn = line.length
                    }
                }

                DOWN -> if ((currentLine < lines.filter { it.isNotEmpty() }.size - 1) && lines[currentLine].isNotEmpty()) {
                    currentLine++
                    terminal.puts(InfoCmp.Capability.cursor_down)
                    // Moving the cursor down resets the location to the start of the line so ensure
                    // it stays in the same column as before, or at the end of the line, whichever is
                    // the lower value.
                    currentColumn = Integer.min(currentColumn, lines[currentLine].length)
                    for (i in 0 until currentColumn) {
                        terminal.puts(InfoCmp.Capability.cursor_right)
                    }
                    terminal.flush()
                }

                LEFT -> if (currentColumn > minColumn) {
                    terminal.puts(InfoCmp.Capability.cursor_left)
                    currentColumn--
                }

                RIGHT -> if (currentColumn < maxColumn && currentColumn < lines[currentLine].length) {
                    terminal.puts(InfoCmp.Capability.cursor_right)
                    currentColumn++
                }

                INSERT -> {
                    // Remap tab to spaces
                    val input = if (editReader.lastBinding == "\t") "    " else editReader.lastBinding
                    input.forEach { c ->
                        if (currentColumn < maxColumn) {
                            val line = lines[currentLine]
                            if (currentColumn == line.length) {
                                lines[currentLine] = line + c
                                print(c)
                                currentColumn++
                            } else {
                                val charsRemaining = line.substring(currentColumn).length
                                lines[currentLine] = line.substring(0, currentColumn) +
                                    c +
                                    line.substring(currentColumn)
                                print(c)
                                print(line.substring(currentColumn))
                                currentColumn++
                                for (i in 0 until charsRemaining) {
                                    terminal.puts(InfoCmp.Capability.cursor_left)
                                }
                                terminal.flush()
                            }
                        } else if (currentLine < maxLine) {
                            terminal.puts(InfoCmp.Capability.cursor_down)
                            print("\r")
                            currentColumn = 0
                            currentLine++
                        }
                    }
                }

                ENTER -> if (currentLine < maxLine) {
                    terminal.puts(InfoCmp.Capability.cursor_down)
                    print("\r")
                    currentColumn = 0
                    currentLine++
                }

                DELETE -> {
                    val line = lines[currentLine]
                    if (line.isNotEmpty()) {
                        if (line.isNotEmpty()) lines[currentLine] = line.removeRange(currentColumn - 1, currentColumn)
                        terminal.puts(InfoCmp.Capability.cursor_left)
                        terminal.puts(InfoCmp.Capability.delete_character)
                        currentColumn--
                    } else if (currentLine > minLine) {
                        currentLine--
                        terminal.puts(InfoCmp.Capability.cursor_up)
                        for (i in 0 until lines[currentLine].length) {
                            terminal.puts(InfoCmp.Capability.cursor_right)
                            currentColumn++
                        }
                    }
                }

                EXIT -> return text ?: ""
                ACCEPT -> break
                else -> { /* no-op */ }
            }
            terminal.flush()
        }
        terminal.attributes = originalAttributes
        return lines.filter { it.isNotEmpty() }.joinToString("\n")
    }

    enum class Operation {
        UP,
        DOWN,
        RIGHT,
        LEFT,
        INSERT,
        ENTER,
        DELETE,
        EXIT,
        ACCEPT,
    }

    private fun bindKeys(): KeyMap<Operation> {
        val map = KeyMap<Operation>()
        map.bind(UP, "\u001B[A")
        map.bind(DOWN, "\u001B[B")
        map.bind(RIGHT, "\u001B[C")
        map.bind(LEFT, "\u001B[D")
        map.bind(ENTER, "\r")
        map.bind(DELETE, "\b")
        map.bind(EXIT, "\u001B")
        map.bind(ACCEPT, KeyMap.ctrl('S'))

        // Allow user to enter characters which are passed to the insert handler.
        for (i in 32..255) {
            // Only bind to insert if the char is not DEL (ASCII 127)
            if (i != 127) map.bind(INSERT, Character.toString(i))
            else map.bind(DELETE, Character.toString(i))
        }

        // Also bind the tab character to insert
        map.bind(INSERT, Character.toString(9))
        return map
    }

}