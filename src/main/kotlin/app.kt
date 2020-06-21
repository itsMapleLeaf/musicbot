
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
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.events.ExceptionEvent
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.EventListener
import kotlin.properties.Delegates.observable

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

    private var radio by observable<Radio?>(null) { _, _, radio ->
        if (radio != null) {
            handleRadioUpdate(radio)
            jda.presence.setPresence(Activity.playing(radio.currentTrack().title), false)
        } else {
            jda.presence.setPresence(Activity.listening("mb radio <youtube link>"), false)
        }
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
                if (event.endReason == AudioTrackEndReason.FINISHED) {
                    seekNextTrack()
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
        val track = radio.currentTrack()

        val nowPlayingEmbed = EmbedBuilder()
            .setTitle(track.title, track.source)
            .build()

        sendMessage(createMessage("now playing:", nowPlayingEmbed))

        lavaPlayerManager.loadItem(track.source, object : AudioLoadResultHandler {
            private fun playTrack(track: AudioTrack) {
                try {
                    audioPlayer.isPaused = false
                    audioPlayer.playTrack(track)
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }

            override fun trackLoaded(track: AudioTrack) {
                playTrack(track)
            }

            override fun playlistLoaded(playlist: AudioPlaylist) {
                playTrack(playlist.tracks.first())
            }

            override fun noMatches() {
                sendMessage(createMessage("couldn't find any matches for this track! trying next one..."))
                seekNextTrack()
            }

            override fun loadFailed(exception: FriendlyException) {
                exception.printStackTrace()
                sendMessage(createMessage("failed to load this track! trying next one..."))
                seekNextTrack()
            }
        })
    }

    private fun handleBotCommand(command: BotCommand) {
        currentChannel = command.event.textChannel

        fun reply(message: Message) {
            command.event.textChannel.sendMessage(message).queue()
        }

        fun joinVoiceChannel() {
            val voiceChannel = command.event.member?.voiceState?.channel
                ?: return reply(createMessage("must be in a voice channel!"))

            command.event.guild.audioManager.apply {
                sendingHandler = jdaSendHandler
                openAudioConnection(voiceChannel)
            }
        }

        when (command.name) {
            "radio" -> {
                joinVoiceChannel()

                val videoId = YouTube.getVideoId(command.argString)
                    ?: return reply(createMessage("couldn't get youtube ID; only youtube links are supported at the moment!"))

                if (!loadNewRadio(videoId)) {
                    reply(createMessage("could not load radio; is this a valid video?"))
                }
            }

            "play", "resume" -> {
                joinVoiceChannel()
                audioPlayer.isPaused = false
            }

            "pause", "stop" -> {
                audioPlayer.isPaused = true
            }

            "skip" -> reply(createMessage("stop trying it doesn't work yet goddAMMIT"))
            "seek" -> reply(createMessage("stop trying it doesn't work yet goddAMMIT"))
            "queue" -> reply(createMessage("stop trying it doesn't work yet goddAMMIT"))
        }
    }

    private fun loadNewRadio(videoId: String): Boolean {
        val video = runBlocking { YouTube.getVideo(videoId) }
            ?: return false

        val relatedVideoList = runBlocking { YouTube.getRelatedVideos(videoId) }

        val firstTrack = RadioTrack(title = video.snippet.title, source = YouTube.getVideoUrl(videoId))

        val tracks = relatedVideoList.items.map { item ->
            RadioTrack(title = item.snippet.title, source = YouTube.getVideoUrl(item.id.videoId))
        }

        this.radio = Radio(tracks = listOf(firstTrack) + tracks, currentIndex = 0)
        return true
    }

    private fun sendMessage(message: Message) {
        currentChannel?.sendMessage(message)?.queue()
    }

    private fun seekNextTrack() {
        val radio = this.radio ?: return
        this.radio = radio.copy(currentIndex = radio.currentIndex + 1)
    }

    private data class BotCommand(
        val name: String,
        val args: List<String>,
        val argString: String,
        val event: MessageReceivedEvent
    )
}
