import discord4j.core.event.domain.message.MessageCreateEvent

private val spacesRegex = Regex("\\s+")

data class CommandContext(val event: MessageCreateEvent, val args: List<String>) {
    fun reply(message: String) {
        event.message.channel.block()?.createMessage(message)?.block()
    }
}

data class Command(
    val name: String,
    val run: CommandContext.() -> Unit
)
