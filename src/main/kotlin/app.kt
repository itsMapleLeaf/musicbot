
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.TrackEndEvent
import com.sedmelluq.discord.lavaplayer.player.event.TrackExceptionEvent
import com.sedmelluq.discord.lavaplayer.player.event.TrackStuckEvent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.UnstableDefault
import java.util.*

val lavaPlayerManager = createLavaPlayerManager()
val audioPlayer: AudioPlayer = lavaPlayerManager.createPlayer()

object AppController {
    private var currentRadio: Radio? = null
    private var currentTrackIndex: Int? = null

    private val isPlaying get() = getCurrentTrack() != null

    fun handleAudioPlayerEvents() {
        audioPlayer.addListener { event ->
            when (event) {
                is TrackEndEvent -> {
                    val radioTrack = getCurrentTrack()
                    if (event.track.identifier == radioTrack?.source) {
                        goToNext()
                        GlobalScope.launch { play() }
                    }
                }
                is TrackExceptionEvent -> {
                    event.exception.printStackTrace()
                }
                is TrackStuckEvent -> {
                    println("track got stuck: ${event.track.info.title}")
                }
            }
        }
    }

    private fun getCurrentTrack(): RadioTrack? {
        val tracks = currentRadio?.tracks ?: return null
        val index = currentTrackIndex ?: return null
        return tracks.getOrNull(index % tracks.size)
    }

    @UnstableDefault
    @ImplicitReflectionSerializer
    suspend fun loadNewRadio(youtubeVideoId: String): NewRadioResult {
        val response = YouTube.getRelatedVideos(youtubeVideoId)
        if (response.items.isEmpty()) {
            return NewRadioResult.NoResults
        }

        val radio = Radio(
            tracks = response.items.map { item ->
                RadioTrack(
                    title = item.snippet.title,
                    source = YouTube.getVideoUrl(item.id.videoId)
                )
            }
        )

        currentRadio = radio
        currentTrackIndex = 0

        return NewRadioResult.Success
    }

    suspend fun play(): PlayResult {
        if (isPlaying) return PlayResult.AlreadyPlaying

        val track = getCurrentTrack() ?: return PlayResult.NoTrack

        return when (val result = lavaPlayerManager.loadItem(track.source)) {
            is AudioLoadResult.TrackLoaded -> {
                audioPlayer.playTrack(result.track)
                PlayResult.Played(track)
            }

            is AudioLoadResult.PlaylistLoaded -> {
                audioPlayer.playTrack(result.playlist.tracks.first())
                PlayResult.Played(track)
            }

            AudioLoadResult.NoMatches ->
                PlayResult.TryNext(track)
        }
    }

//    fun pause() {
//        audioPlayer.isPaused = true
//    }

    fun goToNext() {
        currentTrackIndex = currentTrackIndex?.plus(1)
    }
}

data class Radio(val tracks: List<RadioTrack>)

data class RadioTrack(
    val id: String = uuid(),
    val title: String,
    val source: String
)

enum class NewRadioResult {
    Success,
    NoResults,
}

sealed class PlayResult {
    object AlreadyPlaying : PlayResult()
    object NoTrack : PlayResult()
    data class Played(val track: RadioTrack) : PlayResult()
    data class TryNext(val attemptedToPlay: RadioTrack) : PlayResult()
}

private fun uuid() = UUID.randomUUID().toString()