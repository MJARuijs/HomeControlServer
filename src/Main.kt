import networking.nio.Manager
import networking.nio.Server
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object Main {

    @JvmStatic
    fun main(args: Array<String>) {
        println("Start of program!")
        val manager = Manager()
        val thread = Thread(manager, "Room Manager")
        thread.start()
        Thread.sleep(1000)

        if (!Files.exists(Path.of("connections.txt"))) {
            Files.createFile(Path.of("connections.txt"))
            println("File created!")
        } else {
            println("File already exists!")
        }

        val connections = readConnections()

        val address = try {
            val socket = DatagramSocket()
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002)
            val socketAddress = socket.localAddress.hostAddress
            socket.close()
            socketAddress
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }

        val server = Server(address, 4445, manager, connections)
        manager.register(server)
        server.init()
        println("Server Started at $address")
    }

    private fun readConnections(): ArrayList<String> {
        val connections = ArrayList<String>()

        try {
            val stream = Files.lines(Paths.get("connections.txt"))
            stream.forEach { line -> connections += line }
        } catch (e: Exception) {
            println("FILE COULD NOT BE READ")
            return connections
        }
        println(connections)
        return connections
    }

}
