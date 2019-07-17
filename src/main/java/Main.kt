import networking.PhoneServer
import networking.RoomServer
import networking.nio.Manager
import util.Logger
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object Main {

    @JvmStatic
    fun main(args: Array<String>) {
        Logger.debug("Start of program!")

        val phoneManager = Manager()
        val roomManager = Manager()

        Thread(phoneManager).start()
        Thread(roomManager).start()
        Thread.sleep(100)

        if (!Files.exists(Path.of("connections.txt"))) {
            Files.createFile(Path.of("connections.txt"))
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

        val roomServer = RoomServer(address, 4440, roomManager, connections)
        roomManager.register(roomServer)
        roomServer.init()

        val phoneServer = PhoneServer(address, 4441, phoneManager)
        phoneManager.register(phoneServer)
    }

    private fun readConnections(): ArrayList<String> {
        val connections = ArrayList<String>()

        try {
            val stream = Files.lines(Paths.get("connections.txt"))
            stream.forEach { line -> connections += line }
        } catch (e: Exception) {
            Logger.warn("FILE COULD NOT BE READ")
            return connections
        }
        return connections
    }

}
