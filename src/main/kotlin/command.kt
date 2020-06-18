import net.dv8tion.jda.api.entities.MessageEmbed

private val spacesRegex = Regex("\\s+")

class CommandGroup(prefix: Regex) {
    val commands = mutableMapOf<String, Command>()

    private val prefixAtStart = Regex("^${prefix.pattern}")

    fun findMatchingCommand(inputRaw: String): CommandMatch? {
        val input = inputRaw.replace(spacesRegex, " ")

        val match = prefixAtStart.find(input) ?: return null

        val inputWithoutPrefix = input.drop(match.value.length)

        val (name, command) = commands.entries
            .find { (name) -> inputWithoutPrefix.startsWith(name) }
            ?: return null

        return CommandMatch(command, name, inputWithoutPrefix)
    }
}

data class CommandMatch(
    val command: Command,
    val name: String,
    val inputWithoutPrefix: String
)

class Command(val run: CommandRunFn)

interface CommandContext {
    val args: List<String>
    val argString: String
    fun reply(content: String? = "", embed: MessageEmbed? = null)
}

typealias CommandRunFn = suspend (context: CommandContext) -> Unit

// DSL
fun commandGroup(prefix: Regex, init: CommandGroupScope.() -> Unit) =
    CommandGroup(prefix).apply { init(CommandGroupScope(this)) }

class CommandGroupScope(private val group: CommandGroup) {
    fun command(name: String, run: CommandRunFn) {
        group.commands[name] = Command(run)
    }
}