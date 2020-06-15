interface CommandContext {
    val bot: Bot
    val args: List<String>
    fun reply(message: String)
    fun joinVoiceChannel(): Boolean
}

data class Command(
    val name: String,
    val run: CommandContext.() -> Unit
)

