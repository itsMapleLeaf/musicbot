import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.audio.AudioSendHandler
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.EventListener
import java.nio.Buffer
import java.nio.ByteBuffer

private val token = System.getenv("BOT_TOKEN") ?: error("BOT_TOKEN env variable not found")
private const val commandPrefix = "-"

private val Command.prefixText get() = "$commandPrefix$name"

class Bot(
    private val commands: List<Command>,
    controller: PlayerController
) {
    private val audioPlayer = controller.createAudioPlayer()
    private val jdaSendingHandler = AudioPlayerSendHandler(audioPlayer)

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
                    audioManager.sendingHandler = jdaSendingHandler
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

    fun getCommandPrefix(name: String) = "$commandPrefix$name"
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