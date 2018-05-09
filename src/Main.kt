import client.ArduinoClient
import client.SecureClient
import java.net.InetSocketAddress
import java.nio.channels.SocketChannel

object Main {

    private const val password = "9"

    private var arduino = ArduinoClient("Bedroom", SocketChannel.open(InetSocketAddress("192.168.0.14", 80)))
    private var accessGranted = false

    @JvmStatic
    fun main(args: Array<String>) {
        val server = Server(4444)

        arduino.sendCommand("9")
        println("Server started")

        while (true) {

            val client = SecureClient(server.accept())
            val decodedMessage = client.readMessage()

            if (!accessGranted) {
                if (decodedMessage == password) {
                    accessGranted = true
                    client.writeMessage("ACCESS_GRANTED")
                } else {
                    client.writeMessage("ACCESS_DENIED")
                }
            } else {
                when (decodedMessage) {
                    "close_connection" -> {
                        accessGranted = false
                        client.writeMessage("CONNECTION_CLOSED")
                        client.close()
                    }
                    else -> {
                        val response = arduino.sendCommand(decodedMessage)
                        client.writeMessage(response)
                    }
                }
            }
        }
    }
}
