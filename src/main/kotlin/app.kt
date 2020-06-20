import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventListener
import com.sedmelluq.discord.lavaplayer.player.event.TrackEndEvent
import com.sedmelluq.discord.lavaplayer.player.event.TrackExceptionEvent
import com.sedmelluq.discord.lavaplayer.player.event.TrackStuckEvent
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.UnstableDefault
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.ExceptionEvent
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.EventListener
import kotlin.properties.Delegates

@UnstableDefault
@ImplicitReflectionSerializer
class App {
    private val lavaPlayerManager = createLavaPlayerManager()
    private val audioPlayer: AudioPlayer = lavaPlayerManager.createPlayer()
    private val jdaSendHandler = AudioPlayerSendHandler(audioPlayer)

    private val jda = JDABuilder
        .createDefault(Env.botToken)
        .addEventListeners(getJdaEventListener())
        .build()

    private var currentChannel: MessageChannel? = null

    private var radio by Delegates.observable<Radio?>(null) { _, _, radio ->
        if (radio != null) handleRadioUpdate(radio)
    }

    init {
        audioPlayer.addListener(getAudioPlayerListener())
    }

    private fun getJdaEventListener() = EventListener { event ->
        when (event) {
            is ReadyEvent -> handleReady(event)
            is MessageReceivedEvent -> GlobalScope.launch { handleMessageReceived(event) }
            is ExceptionEvent -> event.cause.printStackTrace()
        }
    }

    private fun getAudioPlayerListener() = AudioEventListener { event ->
        when (event) {
            is TrackEndEvent -> {
                val radio = this.radio
                if (event.endReason == AudioTrackEndReason.FINISHED && radio != null) {
                    this.radio = radio.copy(currentIndex = radio.currentIndex + 1)
                }
            }
            is TrackExceptionEvent ->
                event.exception.printStackTrace()

            is TrackStuckEvent ->
                println("track got stuck: ${event.track.info.title}")

        }
    }

    private fun handleReady(event: ReadyEvent) {
        event.jda.presence.setPresence(Activity.playing("some music"), false)
        println("Ready")
    }

    private fun handleMessageReceived(event: MessageReceivedEvent) {
        currentChannel = event.textChannel

        val content = event.message.contentStripped.replace(Regex("\\s+"), " ").trim()
        if (!content.startsWith("mb")) return

        val words = Regex("\\S+").findAll(content.drop(2)).map { it.value }.toList()
        if (words.isEmpty()) return

        handleBotCommand(
            BotCommand(
                name = words.first(),
                args = words.drop(1),
                argString = words.drop(1).joinToString(" "),
                event = event
            )
        )
    }

    private fun handleRadioUpdate(radio: Radio) {
        lavaPlayerManager.loadItem(radio.currentTrack().source, object : AudioLoadResultHandler {
            override fun loadFailed(exception: FriendlyException) = exception.printStackTrace()

            override fun noMatches() {
                // couldn't load track
            }

            override fun trackLoaded(track: AudioTrack) {
                audioPlayer.playTrack(track)
                currentChannel?.sendMessage(createMessage("now playing: ${track.info.title}"))
            }

            override fun playlistLoaded(playlist: AudioPlaylist) {
                val track = playlist.tracks.first()
                audioPlayer.playTrack(track)
                currentChannel?.sendMessage(createMessage("now playing: ${track.info.title}"))
            }
        })
    }

    private fun handleBotCommand(command: BotCommand) {
        fun reply(text: String? = null, embed: MessageEmbed? = null) {
            command.event.textChannel.sendMessage(createMessage(text, embed)).queue()
        }

        fun joinVoiceChannel() {
            val voiceChannel = command.event.member?.voiceState?.channel
                ?: return reply("must be in a voice channel!")

            command.event.guild.audioManager.apply {
                sendingHandler = jdaSendHandler
                openAudioConnection(voiceChannel)
            }
        }

        when (command.name) {
            "radio" -> {
                val videoId = YouTube.getVideoId(command.argString)
                    ?: return reply("couldn't get youtube ID; only youtube links are supported at the moment!")

                loadNewRadio(videoId)
            }
        }
    }

    private fun loadNewRadio(videoId: String) {
        val response = runBlocking { YouTube.getRelatedVideos(videoId) }

        val tracks = response.items.map { item ->
            RadioTrack(title = item.snippet.title, source = YouTube.getVideoUrl(item.id.videoId))
        }

        this.radio = Radio(tracks = tracks, currentIndex = 0)
    }

    private data class BotCommand(
        val name: String,
        val args: List<String>,
        val argString: String,
        val event: MessageReceivedEvent
    )
}

