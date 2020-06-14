import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats
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
import discord4j.voice.AudioProvider
import java.nio.ByteBuffer

class AudioPlayerWrapper {
    private val playerManager = DefaultAudioPlayerManager()
    private val player = playerManager.createPlayer()
    val provider = LavaPlayerAudioProvider(player)

    init {
        playerManager.configuration.frameBufferFactory =
            AudioFrameBufferFactory { bufferDuration, format, stopping ->
                NonAllocatingAudioFrameBuffer(
                    bufferDuration,
                    format,
                    stopping
                )
            }

        AudioSourceManagers.registerRemoteSources(
            playerManager
        )
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

class LavaPlayerAudioProvider(player: AudioPlayer) :
    AudioProvider(ByteBuffer.allocate(StandardAudioDataFormats.DISCORD_OPUS.maximumChunkSize())) {

    private val player: AudioPlayer
    private val frame = MutableAudioFrame()

    override fun provide(): Boolean {
        // AudioPlayer writes audio data to its AudioFrame
        val didProvide = player.provide(frame)
        // If audio was provided, flip from write-mode to read-mode
        if (didProvide) {
            buffer.flip()
        }
        return didProvide
    }

    init {
        // Allocate a ByteBuffer for Discord4J's AudioProvider to hold audio data for Discord
        // Set LavaPlayer's MutableAudioFrame to use the same buffer as the one we just allocated
        frame.setBuffer(buffer)
        this.player = player
    }
}
