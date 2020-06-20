import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.UnstableDefault
import java.util.*

val lavaPlayerManager = createLavaPlayerManager()
val audioPlayer: AudioPlayer = lavaPlayerManager.createPlayer()

object AppController {
    private var currentRadio: Radio? = null
    private var currentTrackIndex: Int? = null

    private fun getCurrentTrack(): RadioTrack? {
        val index = currentTrackIndex
        return if (index != null) currentRadio?.tracks?.getOrNull(index) else null
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
                PlayResult.TryNext
        }
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

enum class PlayResult {
    NoTrack,
    Played,
    TryNext,
}

private fun uuid() = UUID.randomUUID().toString()