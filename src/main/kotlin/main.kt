fun main() {
    val state = PlayerController()
    Bot(createBotCommands(state)).run()
}
