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
import discord4j.core.DiscordClientBuilder
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.voice.AudioProvider
import java.nio.ByteBuffer

private val botToken = System.getenv("BOT_TOKEN") ?: error("BOT_TOKEN env variable not found")
private const val commandPrefix = ","

fun main() {
    val playerManager = DefaultAudioPlayerManager()

    playerManager.configuration.frameBufferFactory =
        AudioFrameBufferFactory { bufferDuration, format, stopping ->
            NonAllocatingAudioFrameBuffer(
                bufferDuration,
                format,
                stopping
            )
        }

    AudioSourceManagers.registerRemoteSources(playerManager)
    val player = playerManager.createPlayer()
    val provider = LavaPlayerAudioProvider(player)
    val scheduler = TrackScheduler(player)

    val commands = listOf(
        Command("play") {
            val input = args.firstOrNull() ?: return@Command reply("missing youtube link")

            val channel = event.member.get().voiceState.block()?.channel?.block()
                ?: return@Command reply("must be in a voice channel")

            channel.join { spec -> spec.setProvider(provider) }.block()
            playerManager.loadItem(input, scheduler)
        }
    )

    val client = DiscordClientBuilder.create(botToken).build()
        .login().block()
        ?: error("Could not initialize client")

    client.eventDispatcher.on(MessageCreateEvent::class.java).subscribe { event ->
        fun Command.prefixText() = "$commandPrefix$name"

        val content = event.message.content
        val command = commands.find { content.startsWith(it.prefixText()) }

        if (command != null) {
            val args = content
                .drop(command.prefixText().length)
                .split(" ")
                .filterNot { it.isBlank() }
                .map { it.trim() }

            command.run(CommandContext(event, args))
        }
    }

    client.onDisconnect().block()
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

class TrackScheduler(private val player: AudioPlayer) : AudioLoadResultHandler {
    override fun trackLoaded(track: AudioTrack?) {
        // LavaPlayer found an audio source for us to play
        player.playTrack(track)
    }

    override fun playlistLoaded(playlist: AudioPlaylist) {
        // LavaPlayer found multiple AudioTracks from some playlist
    }

    override fun noMatches() {
        // LavaPlayer did not find any audio to extract
    }

    override fun loadFailed(exception: FriendlyException) {
        // LavaPlayer could not parse an audio source for some reason
    }
}