fun createBotCommands(state: PlayerState) = listOf(
    Command("play") {
        val source = args.firstOrNull()
            ?: return@Command reply("missing youtube link")

        if (!joinVoiceChannel()) {
            return@Command reply("must be in a voice channel")
        }

        bot.audioPlayer.play(source)
    }
)