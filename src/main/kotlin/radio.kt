data class Radio(
    val tracks: List<RadioTrack>,
    val currentIndex: Int
) {
    fun currentTrack() = tracks[Math.floorMod(currentIndex, tracks.size)]
}

data class RadioTrack(
    val id: String = uuid(),
    val title: String,
    val source: String
)

