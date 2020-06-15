import java.time.Duration
import java.time.temporal.TemporalAmount

data class Song(
    val id: String,
    val source: String,
    val title: String,
    val duration: Duration
)

data class Playlist(
    val id: String,
    val guildId: String,
    val name: String,
    val songs: List<Song>
)

sealed class CurrentlyPlaying {
    abstract val playingSong: Song
    abstract val position: Duration

    data class FromPlaylist(
        override val playingSong: Song,
        override val position: Duration = Duration.ofMillis(0L),
        val playlist: Playlist
    ) : CurrentlyPlaying()

    data class FromRadio(
        override val playingSong: Song,
        override val position: Duration = Duration.ofMillis(0L),
        val radioRootSong: Song,
        val radioPosition: Int
    ) : CurrentlyPlaying()
}

class PlayerState {
    val playlists = mutableMapOf<String, Playlist>()
    var currentlyPlaying: CurrentlyPlaying? = null
}
