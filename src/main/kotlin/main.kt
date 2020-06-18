
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.UnstableDefault
import net.dv8tion.jda.api.EmbedBuilder

@UnstableDefault
@ImplicitReflectionSerializer
val commands = mapOf(
    "search" to Command { context ->
        val source = context.argString
        if (source.isEmpty()) {
            return@Command context.reply("please provide a link or search query!")
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
)


@UnstableDefault
@ImplicitReflectionSerializer
suspend fun main() {
    try {
        Bot(commands, commandPrefix = Regex("mb\\s")).run()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
