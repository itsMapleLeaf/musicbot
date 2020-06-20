import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrameBufferFactory
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

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

suspend fun AudioPlayerManager.loadItem(identifier: String) =
    suspendCoroutine<AudioLoadResult> { cont ->
        loadItem(identifier, object : AudioLoadResultHandler {
            override fun trackLoaded(track: AudioTrack) {
                cont.resume(AudioLoadResult.TrackLoaded(track))
            }

            override fun playlistLoaded(playlist: AudioPlaylist) {
                cont.resume(AudioLoadResult.PlaylistLoaded(playlist))
            }

            override fun noMatches() {
                cont.resume(AudioLoadResult.NoMatches)
            }

            override fun loadFailed(exception: FriendlyException) {
                cont.resumeWithException(exception)
            }
        })
    }

sealed class AudioLoadResult {
    data class TrackLoaded(val track: AudioTrack) : AudioLoadResult()
    data class PlaylistLoaded(val playlist: AudioPlaylist) : AudioLoadResult()
    object NoMatches : AudioLoadResult()
}