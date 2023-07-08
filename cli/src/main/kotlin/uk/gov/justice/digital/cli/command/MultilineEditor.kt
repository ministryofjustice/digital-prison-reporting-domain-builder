package uk.gov.justice.digital.cli.command

import org.jline.keymap.BindingReader
import org.jline.keymap.KeyMap
import org.jline.terminal.Terminal
import org.jline.utils.InfoCmp

class MultilineEditor(private val terminal: Terminal, private val heading: String) {

    fun run(text: String? = null): String {
        println(heading)
        println()

        val editReader = BindingReader(terminal.reader())

        val map = KeyMap<String>()
        map.bind("up", "\u001B[A", "k")
        map.bind("down", "\u001B[B", "j")
        map.bind("right", "\u001B[C", "j")
        map.bind("left", "\u001B[D", "j")
        map.bind("enter", "\r")
        map.bind("delete", "\b")

        for (i in 32..255) {
            // Only bind if the char is not DEL (ASCII 127)
            if (i != 127) map.bind("insert", Character.toString(i))
            else map.bind("delete", Character.toString(i))
        }

        val minLine = 0
        val maxLine = 20
        val minColumn = 0
        val maxColumn = 79

        var currentLine = 0
        var currentColumn = 0

        val lines = Array(20) { _ -> ""}.toMutableList()

        lines.forEach { println(it) }
        repeat(lines.size) { terminal.puts(InfoCmp.Capability.cursor_up) }
        terminal.flush()

        while(true) {
            val result = editReader.readBinding(map, null, true)
            when (result) {
                "up" -> if (currentLine > minLine) {
                    terminal.puts(InfoCmp.Capability.cursor_up)
                    currentLine--
                    val line = lines[currentLine]

                    // TODO - prevent cursor exceeding line length
                    if (currentColumn > line.length) {
                        print("\u001B[0G")
                        print("\u001B[${line.length + 1}G")
                        currentColumn = line.length
                    }
                }

                "down" -> if ((currentLine < lines.filter { it.isNotEmpty() }.size - 1) && lines[currentLine].isNotEmpty()) {
                    currentLine++
                    terminal.puts(InfoCmp.Capability.cursor_down)
                    // Moving the cursor down resets the location to the start of the line so ensure
                    // it stays in the same column as before, or at the end of the line, whichever is
                    // the lower value.
                    currentColumn = Integer.min(currentColumn, lines[currentLine].length)
                    for (i in 0 until currentColumn) { terminal.puts(InfoCmp.Capability.cursor_right) }
                    terminal.flush()
                }

                "left" -> if (currentColumn > minColumn) {
                    terminal.puts(InfoCmp.Capability.cursor_left)
                    currentColumn--
                }
                "right" -> if (currentColumn < maxColumn && currentColumn < lines[currentLine].length) {
                    terminal.puts(InfoCmp.Capability.cursor_right)
                    currentColumn++
                }
                "insert" -> {
                    if (currentColumn < maxColumn) {
                        val line = lines[currentLine]
                        if (currentColumn == line.length) {
                            lines[currentLine] = line + editReader.lastBinding
                            print(editReader.lastBinding)
                            currentColumn++
                        }
                        else {
                            val charsRemaining = line.substring(currentColumn).length
                            lines[currentLine] = line.substring(0, currentColumn) +
                                editReader.lastBinding +
                                line.substring(currentColumn)
                            print(editReader.lastBinding)
                            print(line.substring(currentColumn))
                            currentColumn++
                            for (i in 0 until charsRemaining) { terminal.puts(InfoCmp.Capability.cursor_left) }
                            terminal.flush()
                        }
                    }
                    else if (currentLine < maxLine) {
                        terminal.puts(InfoCmp.Capability.cursor_down)
                        print("\r")
                        currentColumn = 0
                        currentLine++
                    }
                }
                "enter" -> if (currentLine < maxLine) {
                    terminal.puts(InfoCmp.Capability.cursor_down)
                    print("\r")
                    currentColumn = 0
                    currentLine++
                }
                "delete" -> {
                    val line = lines[currentLine]
                    if (line.isNotEmpty()) {
                        if (line.isNotEmpty()) lines[currentLine] = line.removeRange(currentColumn - 1, currentColumn)
                        terminal.puts(InfoCmp.Capability.cursor_left)
                        terminal.puts(InfoCmp.Capability.delete_character)
                        currentColumn--
                    }
                    else if (currentLine > minLine) {
                        currentLine--
                        terminal.puts(InfoCmp.Capability.cursor_up)
                        for(i in 0 until lines[currentLine].length) {
                            terminal.puts(InfoCmp.Capability.cursor_right)
                            currentColumn++
                        }
                    }
                }
            }
            print("\u001B7") // Save cursor pos
            print("\u001B[24;0H")
            print("line: $currentLine column: $currentColumn currentLineLength: ${lines[currentLine].length}                   ")
            print("\u001B8") // restore saved cursor pos
            terminal.flush()
        }
        return lines.joinToString("\n")
    }

}