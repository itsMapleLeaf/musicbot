import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.UnstableDefault
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed

private val lavaPlayerManager = createLavaPlayerManager()
private val audioPlayer = lavaPlayerManager.createPlayer()


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

//        val relatedVideosUrl =
//            "https://www.googleapis.com/youtube/v3/" +
//                    "?key=${safeGetEnv("GOOGLE_API_KEY")}" +
//                    "&relatedToVideoId=$videoId" +
//                    "&part=snippet" +
//                    "&type=video" +
//                    "&videoSyndicated=true" +
//                    "&maxResults=50"
//
//        when (val result = lavaPlayerManager.loadItem(relatedVideosUrl)) {
//            is AudioLoadResult.TrackLoaded ->
//                context.reply("loaded track \"${result.track.info.title}\"")
//
//            is AudioLoadResult.PlaylistLoaded ->
//                context.reply("loaded playlist with ${result.playlist.tracks.size} tracks")
//
//            AudioLoadResult.NoMatches ->
//                context.reply("no matches :(")
//        }

        val response = YouTube.getRelatedVideos(videoId)
        context.reply("found ${response.items.size} related tracks", createSearchResponseEmbed(response))
    }

    command("play") {}

    command("pause") {}

    command("skip") {}

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