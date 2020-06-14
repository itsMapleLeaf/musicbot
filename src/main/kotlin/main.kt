import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame
import discord4j.core.DiscordClientBuilder
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.voice.AudioProvider
import java.nio.ByteBuffer

private val botToken = System.getenv("BOT_TOKEN") ?: error("BOT_TOKEN env variable not found")
private const val commandPrefix = ","

val player = AudioPlayerWrapper()

val commands = listOf(
    Command("play") {
        val input = args.firstOrNull() ?: return@Command reply("missing youtube link")

        val channel = event.member.get().voiceState.block()?.channel?.block()
            ?: return@Command reply("must be in a voice channel")

        channel.join { spec -> spec.setProvider(player.provider) }.block()
        player.play(input)
    }
)

fun main() {
    val client = DiscordClientBuilder.create(botToken).build()
        .login().block()
        ?: error("Could not initialize client")

    client.eventDispatcher.on(MessageCreateEvent::class.java).subscribe { event ->
        fun Command.prefixText() = "$commandPrefix$name"

        val content = event.message.content
        val command = commands.find { content.startsWith(it.prefixText()) }

        if (command != null) {
            val args = content
                .drop(command.prefixText().length)
                .split(" ")
                .filterNot { it.isBlank() }
                .map { it.trim() }

            command.run(CommandContext(event, args))
        }
    }

    client.onDisconnect().block()
}

