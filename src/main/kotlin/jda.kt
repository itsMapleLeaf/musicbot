import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.audio.AudioSendHandler
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.hooks.EventListener
import java.nio.Buffer
import java.nio.ByteBuffer

fun JDA.eventChannel(): ReceiveChannel<GenericEvent> {
    val channel = Channel<GenericEvent>()
    addEventListener(EventListener { event -> channel.offer(event) })
    return channel
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
