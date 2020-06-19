import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.UnstableDefault
import net.dv8tion.jda.api.EmbedBuilder

@UnstableDefault
@ImplicitReflectionSerializer
suspend fun main() {
    Bot(commands).run()
}

@UnstableDefault
@ImplicitReflectionSerializer
val commands = commandGroup(prefix = Regex("mb\\s")) {
    command("search") { context ->
        val source = context.argString
        if (source.isEmpty()) {
            return@command context.reply("please provide a search query!")
        }

        val data = YouTube.searchVideos(source)

        val embed = EmbedBuilder()
        for ((index, item) in data.items.withIndex()) {
            val channelTitle = item.snippet.channelTitle.markdownEscape()
            val videoTitle = item.snippet.title.markdownEscape()
            val videoUrl = YouTube.getVideoUrl(item.id.videoId)
            embed.addField(channelTitle, "**`${index + 1}` [$videoTitle]($videoUrl)**", false)

        }

        context.reply("found these results:", embed.build())
    }
}
