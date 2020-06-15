import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

fun createBotCommands(controller: PlayerController) = listOf(
    Command("summon") {
        if (!joinVoiceChannel()) {
            return@Command reply("must be in a voice channel")
        }
    },

    Command("add") {
        val source = args.firstOrNull()
            ?: return@Command reply("missing youtube link")

        val playlist = controller.currentPlaylist()
        if (playlist == null) {
            val playlistNames = controller.playlistNames()
            if (playlistNames.isEmpty()) {
                reply("create a playlist first! run ${bot.getCommandPrefix("create")} <name>")
            } else {
                reply(
                    "switch to a playlist first!\n" +
                            "run ${bot.getCommandPrefix("set")} <name>\n" +
                            "available playlists: ${playlistNames.joinToString(", ")}"
                )
            }
            return@Command
        }

        GlobalScope.launch {
            when (val result = controller.loadSongFromExternalSource(source)) {
                is AudioLoadResult.Loaded -> {
                    // ???
                }
                is AudioLoadResult.LoadedPlaylist -> reply("playlists are not supported yet, sorry!")
                is AudioLoadResult.NoMatches -> reply("no matches found")
                is AudioLoadResult.Failed -> reply("an error occurred")
            }
        }
    },

    Command("set") {},

    Command("create") {}
)