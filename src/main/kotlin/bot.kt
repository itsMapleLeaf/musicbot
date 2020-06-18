import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class Bot(private val commands: CommandGroup) {
    private val lavaPlayerManager = createLavaPlayerManager()
    private val audioPlayer = lavaPlayerManager.createPlayer()
    private val jdaSendingHandler = AudioPlayerSendHandler(audioPlayer)

    private fun handleReady(event: ReadyEvent) {
        event.jda.presence.setPresence(Activity.playing("psytrance lol"), false)
        println("Ready")
    }

    private suspend fun handleMessageReceived(event: MessageReceivedEvent) {
        val match = commands.findMatchingCommand(event.message.contentStripped) ?: return

        match.command.run(object : CommandContext {
            override val argString = match.inputWithoutPrefix.drop(match.name.length)
            override val args = argString.split(" ")

            override fun reply(content: String?, embed: MessageEmbed?) {
                val message = MessageBuilder().apply {
                    if (content != null) setContent(content)
                    if (embed != null) setEmbed(embed)
                }.build()

                event.textChannel.sendMessage(message).queue()
            }
        })
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

