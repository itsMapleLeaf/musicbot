import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.UnstableDefault

@UnstableDefault
@ImplicitReflectionSerializer
suspend fun main() {
    try {
        Bot().run()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
