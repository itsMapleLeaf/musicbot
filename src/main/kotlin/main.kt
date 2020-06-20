
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.UnstableDefault
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed

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

        when (AppController.loadNewRadio(videoId)) {
            NewRadioResult.Success ->
                context.reply("radio loaded! run `mb play` to start (i'll eventually do this automatically)")

            NewRadioResult.NoResults ->
                context.reply("couldn't get youtube ID; only youtube links are supported at the moment!")
        }
    }

    command("play") {}

    command("pause") {}

    command("skip") {}

    command("queue") {}

    command("search") { context ->
        val source = context.argString
        if (source.isEmpty()) {
            return@command context.reply("please provide a search query!")
        }

        val response = YouTube.searchVideos(source)
        context.reply("found these results:", createSearchResponseEmbed(response))
    }
}

@UnstableDefault
@ImplicitReflectionSerializer
suspend fun main() {
    Bot(commands, AudioPlayerSendHandler(audioPlayer)).run()
}