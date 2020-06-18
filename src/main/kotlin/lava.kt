import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrameBufferFactory
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer

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
