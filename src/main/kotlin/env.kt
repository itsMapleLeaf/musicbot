fun safeGetEnv(name: String) =
    System.getenv(name) ?: error("env variable not found: $name")

object Env {
    val botToken = safeGetEnv("DISCORD_BOT_TOKEN")
    val googleApiKey = safeGetEnv("GOOGLE_API_KEY")
}
