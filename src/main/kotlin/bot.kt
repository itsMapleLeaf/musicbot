import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrameBufferFactory
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import java.nio.Buffer
import java.nio.ByteBuffer
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.UnstableDefault
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.audio.AudioSendHandler
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.EventListener

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

fun JDA.eventChannel(): ReceiveChannel<GenericEvent> {
    val channel = Channel<GenericEvent>()
    addEventListener(EventListener { event -> channel.offer(event) })
    return channel
}

fun String.markdownEscape() =
    replace(Regex("[_~*]")) { "\\${it.value}" }

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
