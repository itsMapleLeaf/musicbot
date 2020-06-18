import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.coroutines.awaitObject
import com.github.kittinunf.fuel.serialization.kotlinxDeserializerOf
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json

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
        val channelTitle: String
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

    fun getVideoUrl(videoId: String) = "https://youtu.be/$videoId"
}
