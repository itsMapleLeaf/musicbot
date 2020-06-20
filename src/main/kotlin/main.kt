
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.UnstableDefault
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed

val app = AppController()

@UnstableDefault
@ImplicitReflectionSerializer
val commands = commandGroup(prefix = Regex("mb\\s")) {
    fun createSearchResponseEmbed(response: YouTube.SearchResponse): MessageEmbed {
        val embed = EmbedBuilder()
        for ((index, item) in response.items.withIndex()) {
            val channelTitle = item.snippet.channelTitle.markdownEscape()
            val videoTitle = item.snippet.title.markdownEscape()
            val videoUrl = YouTube.getVideoUrl(item.id.videoId)
            embed.addField(channelTitle, "**`${index + 1}` [$videoTitle]($videoUrl)**", false)
        }
        return embed.build()
    }

    command("radio") { context ->
        val videoId = YouTube.getVideoId(context.argString)
            ?: return@command context.reply("couldn't get youtube ID; only youtube links are supported at the moment!")

        if (app.loadNewRadio(videoId) == NewRadioResult.NoResults) {
            context.reply("couldn't get youtube ID; only youtube links are supported at the moment!")
            return@command
        }

        context.joinVoiceChannel()

        val result = app.play(
            onTryNext = { track ->
                context.reply("couldn't play ${track.title}, trying next...")
            }
        )

        when (result) {
            PlayResult.NoTrack -> {
                context.reply("no track to play! start a radio first")
            }
        }
    }

    command("play") {
        app.resume()
    }

    command("pause") {
        app.pause()
    }

    command("skip") { context -> context.reply("stop trying it doesn't work yet goddAMMIT") }

    command("queue") { context -> context.reply("stop trying it doesn't work yet goddAMMIT") }

//    command("search") { context ->
//        val source = context.argString
//        if (source.isEmpty()) {
//            return@command context.reply("please provide a search query!")
//        }
//
//        val response = YouTube.searchVideos(source)
//        context.reply("found these results:", createSearchResponseEmbed(response))
//    }
}

@ExperimentalCoroutinesApi
@UnstableDefault
@ImplicitReflectionSerializer
suspend fun main() {
    app.handleAudioPlayerEvents()

    val bot = Bot(commands, AudioPlayerSendHandler(audioPlayer))

    GlobalScope.launch {
        for (event in app.events) {
            when (event) {
                is AppController.Event.PlayedTrack -> {
                    bot.sendMessageInBoundChannel("playing: ${event.track.title}")
                }
            }
        }
    }.invokeOnCompletion { e -> e?.printStackTrace() }

    bot.run()
}