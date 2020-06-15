import com.github.kittinunf.fuel.Fuel
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrameBufferFactory
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer
import kotlinx.coroutines.*
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.audio.AudioSendHandler
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.EventListener
import java.nio.Buffer
import java.nio.ByteBuffer

private fun safeGetEnv(name: String) =
    System.getenv(name) ?: error("env variable not found: $name")

private val token = safeGetEnv("BOT_TOKEN")
private val youtubeApiKey = safeGetEnv("YOUTUBE_API_KEY")
private const val commandPrefix = "-"

class Bot {
    private val lavaPlayerManager = createLavaPlayerManager()
    private val audioPlayer = lavaPlayerManager.createPlayer()
    private val jdaSendingHandler = AudioPlayerSendHandler(audioPlayer)

    private fun handleReady(event: ReadyEvent) {
        println("Ready")
        event.jda.presence.setPresence(Activity.playing("psytrance"), false)
    }

    private fun handleMessageReceived(event: MessageReceivedEvent) {
        val content = event.message.contentStripped

        fun reply(message: String) {
            event.message.channel.sendMessage(message)
        }

        if (content.startsWith(commandPrefix)) {
            val contentWithoutPrefix = content.drop(commandPrefix.length)
            val words = contentWithoutPrefix.split(Regex("\\s+"))
            val command = words[0]
            val args = words.drop(1)

            when (command) {
                "radio" -> {
                    val source = args.firstOrNull()
                        ?: return reply("please provide a link or search query! e.g. \"${commandPrefix}radio <link/query>\"")


                }
            }
        }
    }

    fun run() {
        JDABuilder
            .createDefault(token)
            .addEventListeners(EventListener { event ->
                if (event is ReadyEvent) handleReady(event)
                if (event is MessageReceivedEvent) handleMessageReceived(event)
            })
            .build()
    }

    fun getCommandPrefix(name: String) = "$commandPrefix$name"
}

fun createLavaPlayerManager(): AudioPlayerManager {
    val lavaPlayerManager = DefaultAudioPlayerManager()

    lavaPlayerManager.configuration.frameBufferFactory =
        AudioFrameBufferFactory { bufferDuration, format, stopping ->
            NonAllocatingAudioFrameBuffer(
                bufferDuration,
                format,
                stopping
            )
        }

    AudioSourceManagers.registerRemoteSources(lavaPlayerManager)

    return lavaPlayerManager
}

/**
 * This is a wrapper around AudioPlayer which makes it behave as an AudioSendHandler for JDA. As JDA calls canProvide
 * before every call to provide20MsAudio(), we pull the frame in canProvide() and use the frame we already pulled in
 * provide20MsAudio().
 */
class AudioPlayerSendHandler(private val audioPlayer: AudioPlayer) : AudioSendHandler {
    private val buffer = ByteBuffer.allocate(1024)
    private val frame = MutableAudioFrame()

    override fun canProvide(): Boolean {
        // returns true if audio was provided
        return audioPlayer.provide(frame)
    }

    override fun provide20MsAudio(): ByteBuffer? {
        // flip to make it a read buffer
        (buffer as Buffer).flip()
        return buffer
    }

    override fun isOpus(): Boolean {
        return true
    }

    /**
     * @param audioPlayer Audio player to wrap.
     */
    init {
        frame.setBuffer(buffer)
    }
}
