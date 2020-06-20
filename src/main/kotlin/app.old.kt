
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.TrackEndEvent
import com.sedmelluq.discord.lavaplayer.player.event.TrackExceptionEvent
import com.sedmelluq.discord.lavaplayer.player.event.TrackStuckEvent
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.UnstableDefault
import java.util.*

class AppController {
    val lavaPlayerManager = createLavaPlayerManager()
    val audioPlayer: AudioPlayer = lavaPlayerManager.createPlayer()

    private var currentRadio: Radio? = null
    private var currentTrackIndex: Int? = null

    val events = Channel<Event>()

    fun handleAudioPlayerEvents() {
        audioPlayer.addListener { event ->
            when (event) {
                is TrackEndEvent -> {
                    if (event.endReason == AudioTrackEndReason.FINISHED) {
                        goToNext()
                        GlobalScope.launch { play() }.invokeOnCompletion { it?.printStackTrace() }
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

    suspend fun play(onTryNext: (suspend (RadioTrack) -> Unit)? = {}): PlayResult {
        val track = getCurrentTrack() ?: return PlayResult.NoTrack

        val loadResult = lavaPlayerManager.loadItem(track.source)
        if (loadResult == AudioLoadResult.NoMatches) {
            onTryNext?.invoke(track)
            goToNext()
            return play(onTryNext)
        }

        val audioTrack = when (loadResult) {
            is AudioLoadResult.TrackLoaded -> loadResult.track
            is AudioLoadResult.PlaylistLoaded -> loadResult.playlist.tracks.first()
            else -> error("this shouldn't happen")
        }

        audioPlayer.playTrack(audioTrack)
        events.offer(Event.PlayedTrack(track))
        return PlayResult.Played(track)
    }

    fun pause() {
        audioPlayer.isPaused = true
    }

    fun resume() {
        audioPlayer.isPaused = false
    }

    private fun goToNext() {
        currentTrackIndex = currentTrackIndex?.plus(1)
    }

    sealed class Event {
        data class PlayedTrack(val track: RadioTrack) : Event()
    }
}

data class Radio(
    val tracks: List<RadioTrack>,
    val currentIndex: Int
)

fun Radio.currentTrack() = tracks[currentIndex % tracks.size]

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
    data class Played(val track: RadioTrack) : PlayResult()
}

private fun uuid() = UUID.randomUUID().toString()