import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.EventListener

private val token = System.getenv("BOT_TOKEN") ?: error("BOT_TOKEN env variable not found")
private const val commandPrefix = "-"

private val Command.prefixText get() = "$commandPrefix$name"

class Bot(private val commands: List<Command>) {
    internal val audioPlayer = AudioPlayerWrapper()

    private fun handleReady(event: ReadyEvent) {
        println("Ready")
        event.jda.presence.setPresence(Activity.playing("psytrance"), false)
    }

    private fun handleMessageReceived(event: MessageReceivedEvent) {
        val content = event.message.contentStripped
        val command = commands.find { content.startsWith(it.prefixText) }

        if (command != null) {
            val args = content
                .drop(command.prefixText.length)
                .split(" ")
                .filterNot { it.isBlank() }
                .map { it.trim() }

            val bot = this

            val context = object : CommandContext {
                override val bot = bot
                override val args = args

                override fun reply(message: String) {
                    event.channel.sendMessage(message)
                }

                override fun joinVoiceChannel(): Boolean {
                    val voiceState = event.member?.voiceState ?: return false
                    val channel = voiceState.channel ?: return false
                    val guild = voiceState.guild
                    val audioManager = guild.audioManager
                    audioManager.sendingHandler = audioPlayer.jdaSendingHandler
                    audioManager.openAudioConnection(channel)
                    return true
                }
            }

            command.run(context)
        }
    }

    private val eventListener = EventListener { event ->
        if (event is ReadyEvent) handleReady(event)
        if (event is MessageReceivedEvent) handleMessageReceived(event)
    }

    fun run() {
        JDABuilder
            .createDefault(token)
            .addEventListeners(eventListener)
            .build()
            .awaitReady()
    }
}
