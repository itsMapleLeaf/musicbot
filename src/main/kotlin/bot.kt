
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class Bot(
    private val commands: Map<String, Command>,
    private val commandPrefix: Regex
) {
    private val lavaPlayerManager = createLavaPlayerManager()
    private val audioPlayer = lavaPlayerManager.createPlayer()
    private val jdaSendingHandler = AudioPlayerSendHandler(audioPlayer)

    private fun handleReady(event: ReadyEvent) {
        event.jda.presence.setPresence(Activity.playing("psytrance lol"), false)
        println("Ready")
    }

    private suspend fun handleMessageReceived(event: MessageReceivedEvent) {
        val content = event.message.contentStripped.replace(Regex("\\s+"), " ")
        val match = commandPrefix.find(content)
        if (match == null || match.range.first != 0) return

        val contentWithoutPrefix = content.drop(match.value.length)

        val commandEntry = commands.entries.find { (name) ->
            contentWithoutPrefix.startsWith(name)
        }

        if (commandEntry != null) {
            val (name, command) = commandEntry

            val context = object : CommandContext {
                override val argString = contentWithoutPrefix.drop(name.length)
                override val args = argString.split(" ")

                override fun reply(content: String?, embed: MessageEmbed?) {
                    val message = MessageBuilder().apply {
                        if (content != null) setContent(content)
                        if (embed != null) setEmbed(embed)
                    }.build()

                    event.textChannel.sendMessage(message).queue()
                }
            }

            command.run(context)
        }
    }

    suspend fun run() {
        val jda = JDABuilder.createDefault(Env.botToken).build()
        for (event in jda.eventChannel()) {
            when (event) {
                is ReadyEvent -> handleReady(event)
                is MessageReceivedEvent -> handleMessageReceived(event)
            }
        }
    }
}

class Command(
    val run: suspend (context: CommandContext) -> Unit
)

interface CommandContext {
    val args: List<String>
    val argString: String
    fun reply(content: String? = "", embed: MessageEmbed? = null)
}
