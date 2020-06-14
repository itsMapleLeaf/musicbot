import club.minnced.jda.reactor.ReactiveEventManager
import club.minnced.jda.reactor.on
import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame
import lavalink.client.io.jda.JdaLavalink
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.audio.AudioSendHandler
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.EventListener
import java.nio.ByteBuffer
import java.util.*

private val botToken = System.getenv("BOT_TOKEN") ?: error("BOT_TOKEN env variable not found")
private const val commandPrefix = ","

val player = AudioPlayerWrapper()

val commands = listOf(
    Command("play") {
        val input = args.firstOrNull() ?: return@Command reply("missing youtube link")

        if (!joinVoiceChannel()) return@Command reply("must be in a voice channel")

        player.play(input)
    }
)

val eventListener = object : EventListener {
    override fun onEvent(event: GenericEvent) {
        if (event is ReadyEvent) {
            println("Ready")
            event.jda.presence.setPresence(Activity.playing("psytrance"), false)
        }

        if (event is MessageReceivedEvent) {
            fun Command.prefixText() = "$commandPrefix$name"

            val content = event.message.contentStripped
            val command = commands.find { content.startsWith(it.prefixText()) }

            if (command != null) {
                val args = content
                    .drop(command.prefixText().length)
                    .split(" ")
                    .filterNot { it.isBlank() }
                    .map { it.trim() }

                command.run(object : CommandContext {
                    override val args = args

                    override fun reply(message: String) {
                        event.channel.sendMessage(message)
                    }

                    override fun joinVoiceChannel(): Boolean {
                        val voiceState = event.member?.voiceState ?: return false
                        val channel = voiceState.channel ?: return false
                        val guild = voiceState.guild
                        val audioManager = guild.audioManager
                        audioManager.sendingHandler = player.jdaSendingHandler
                        audioManager.openAudioConnection(channel)
                        return true
                    }
                })
            }
        }
    }
}

fun main() {
    val client = JDABuilder(botToken).addEventListeners(eventListener).build()
    client.awaitReady()
}

