
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.coroutines.awaitObject
import com.github.kittinunf.fuel.serialization.kotlinxDeserializerOf
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import java.net.MalformedURLException
import java.net.URL

object YouTube {
    private val client = FuelManager().apply {
        basePath = "https://www.googleapis.com/youtube/v3"
        baseParams = listOf("key" to safeGetEnv("GOOGLE_API_KEY"))
    }

    // TODO: look into some less annoying way of serialization. too much repetition here
    @UnstableDefault
    @ImplicitReflectionSerializer
    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Serializable
    data class VideoList(
        val items: List<Video>
    )

    @Serializable
    data class VideoListWithExtendedId(
        val items: List<VideoWithExtendedId>
    )

    abstract class VideoBase {
        abstract val snippet: VideoSnippet
    }

    @Serializable
    data class Video(
        val id: String,
        override val snippet: VideoSnippet
    ) : VideoBase()

    @Serializable
    data class VideoWithExtendedId(
        val id: VideoExtendedId,
        override val snippet: VideoSnippet
    ) : VideoBase()

    @Serializable
    data class VideoExtendedId(
        val videoId: String
    )

    @Serializable
    data class VideoSnippet(
        val title: String,
        val channelTitle: String,
        val liveBroadcastContent: String
    )

    @UnstableDefault
    @ImplicitReflectionSerializer
    suspend fun getVideo(videoId: String): Video? {
        val params = listOf(
            "id" to videoId,
            "part" to "snippet"
        )

        val result = client.get("/videos", params)
            .awaitObject<VideoList>(kotlinxDeserializerOf(json))

        return result.items.firstOrNull()
    }

    @UnstableDefault
    @ImplicitReflectionSerializer
    suspend fun searchVideos(query: String): VideoListWithExtendedId {
        val params = listOf(
            "part" to "snippet",
            "q" to query,
            "type" to "video",
            "videoSyndicated" to "true",
            "maxResults" to "10"
        )

        return client.get("/search", params).awaitObject(kotlinxDeserializerOf(json))
    }

    @UnstableDefault
    @ImplicitReflectionSerializer
    suspend fun getRelatedVideos(videoId: String): VideoListWithExtendedId {
        val params = listOf(
            "part" to "snippet",
            "relatedToVideoId" to videoId,
            "type" to "video",
            "videoSyndicated" to "true",
            "maxResults" to "50"
        )

        val response = client.get("/search", params)
            .awaitObject<VideoListWithExtendedId>(kotlinxDeserializerOf(json))

        return response.copy(items = response.items.filter { it.snippet.liveBroadcastContent == "none" })
    }

    fun getVideoUrl(videoId: String) = "https://youtu.be/$videoId"

    fun getVideoId(urlString: String): String? {
        val url = try {
            URL(urlString.replace(Regex("^(https?://)?"), "https://"))
        } catch (e: MalformedURLException) {
            return null
        }

        return when {
            url.host.endsWith("youtube.com") && url.path == "/watch" ->
                parseQueryString(url.query)["v"]

            url.host.endsWith("youtu.be") ->
                url.path.removePrefix("/")

            else -> null
        }
    }
}
