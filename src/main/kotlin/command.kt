//import discord4j.core.event.domain.message.MessageCreateEvent

private val spacesRegex = Regex("\\s+")

interface CommandContext {
    val args: List<String>
    fun reply(message: String)
    fun joinVoiceChannel(): Boolean
}

data class Command(
    val name: String,
    val run: CommandContext.() -> Unit
)
