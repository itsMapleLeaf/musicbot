import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrameBufferFactory
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer
import net.dv8tion.jda.api.audio.AudioSendHandler
import java.nio.Buffer
import java.nio.ByteBuffer

class AudioPlayerWrapper {
    private val playerManager = DefaultAudioPlayerManager()
    private val player = playerManager.createPlayer()
    val jdaSendingHandler = AudioPlayerSendHandler(player)

    init {
        playerManager.configuration.frameBufferFactory =
            AudioFrameBufferFactory { bufferDuration, format, stopping ->
                NonAllocatingAudioFrameBuffer(
                    bufferDuration,
                    format,
                    stopping
                )
            }

        AudioSourceManagers.registerRemoteSources(playerManager)
    }

    fun play(source: String) {
        playerManager.loadItem(source, object : AudioLoadResultHandler {
            override fun trackLoaded(track: AudioTrack) {
                player.playTrack(track)
            }

            override fun loadFailed(exception: FriendlyException?) {}
            override fun noMatches() {}
            override fun playlistLoaded(playlist: AudioPlaylist?) {}
        })
    }
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