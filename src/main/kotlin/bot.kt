import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.UnstableDefault
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

private const val commandPrefix = ".."

@UnstableDefault
@ImplicitReflectionSerializer
class Bot {
    private val lavaPlayerManager = createLavaPlayerManager()
    private val audioPlayer = lavaPlayerManager.createPlayer()
    private val jdaSendingHandler = AudioPlayerSendHandler(audioPlayer)

    private fun handleReady(event: ReadyEvent) {
        event.jda.presence.setPresence(Activity.playing("psytrance lol"), false)
        println("Ready")
    }

    private suspend fun handleMessageReceived(event: MessageReceivedEvent) {
        val content = event.message.contentStripped

        fun reply(content: String? = "", embed: MessageEmbed? = null) {
            val message = MessageBuilder().apply {
                if (content != null) setContent(content)
                if (embed != null) setEmbed(embed)
            }.build()

            event.textChannel.sendMessage(message).queue()
        }

        if (content.startsWith(commandPrefix)) {
            val contentWithoutPrefix = content.drop(commandPrefix.length)
            val words = contentWithoutPrefix.split(Regex("\\s+"))
            val command = words[0]
            val args = words.drop(1)

            when (command) {
                "radio" -> {
                    val source = args.joinToString(" ")
                    if (source.isEmpty()) {
                        return reply("please provide a link or search query! e.g. \"${commandPrefix}radio <link/query>\"")
                    }

                    val data = YouTube.searchVideos(source)

                    val embed = EmbedBuilder()
                    for ((index, item) in data.items.withIndex()) {
                        embed.addField(
                            item.snippet.channelTitle.markdownEscape(),
                            "**`${index + 1}` [${item.snippet.title.markdownEscape()}](https://youtu.be/${item.id.videoId})**",
                            false
                        )
                    }

                    reply("found these results:", embed.build())
                }
            }
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

