fun main() {
    val state = PlayerState()
    Bot(createBotCommands(state)).run()
}
