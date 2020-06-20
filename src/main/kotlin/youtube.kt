import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.coroutines.awaitObject
import com.github.kittinunf.fuel.serialization.kotlinxDeserializerOf
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import java.net.MalformedURLException
import java.net.URL

@UnstableDefault
@ImplicitReflectionSerializer
object YouTube {
    private val client = FuelManager().apply {
        basePath = "https://www.googleapis.com/youtube/v3"
        baseParams = listOf("key" to safeGetEnv("GOOGLE_API_KEY"))
    }

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Serializable
    data class SearchResponse(
        val items: List<SearchResponseItem>
    )

    @Serializable
    data class SearchResponseItem(
        val id: SearchResponseItemId,
        val snippet: SearchResponseItemSnippet
    )

    @Serializable
    data class SearchResponseItemId(
        val videoId: String
    )

    @Serializable
    data class SearchResponseItemSnippet(
        val title: String,
        val channelTitle: String,
        val liveBroadcastContent: String
    )

    suspend fun searchVideos(query: String): SearchResponse {
        val params = listOf(
            "part" to "snippet",
            "q" to query,
            "type" to "video",
            "videoSyndicated" to "true",
            "relevanceLanguage" to "en",
            "maxResults" to "10"
        )

        return client.get("/search", params).awaitObject(kotlinxDeserializerOf(json))
    }

    suspend fun getRelatedVideos(videoId: String): SearchResponse {
        val params = listOf(
            "part" to "snippet",
            "relatedToVideoId" to videoId,
            "type" to "video",
            "videoSyndicated" to "true",
            "relevanceLanguage" to "en",
            "maxResults" to "50"
        )

        val response = client.get("/search", params)
            .awaitObject<SearchResponse>(kotlinxDeserializerOf(json))

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
