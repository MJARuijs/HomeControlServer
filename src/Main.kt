import networking.nio.Manager
import networking.nio.Server

object Main {

    @JvmStatic
    fun main(args: Array<String>) {
        println("Start of program")
        val manager = Manager()
        val thread = Thread(manager, "Room Manager")
        thread.start()
        Thread.sleep(1000)

        val server = Server(4443, manager)
        manager.register(server)
        println("Server Started")
    }

}
