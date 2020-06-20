
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.TrackEndEvent
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

    fun handleAudioPlayerEvents() {
        audioPlayer.addListener { event ->
            when (event) {
                is TrackEndEvent -> {
                    goToNext()
                    GlobalScope.launch { play() }
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
        val track = getCurrentTrack() ?: return PlayResult.NoTrack

        return when (val result = lavaPlayerManager.loadItem(track.source)) {
            is AudioLoadResult.TrackLoaded -> {
                audioPlayer.playTrack(result.track)
                PlayResult.Played
            }

            is AudioLoadResult.PlaylistLoaded -> {
                audioPlayer.playTrack(result.playlist.tracks.first())
                PlayResult.Played
            }

            AudioLoadResult.NoMatches ->
                PlayResult.TryNext(track)
        }
    }

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
    object NoTrack : PlayResult()
    object Played : PlayResult()
    data class TryNext(val attemptedToPlay: RadioTrack) : PlayResult()
}

private fun uuid() = UUID.randomUUID().toString()