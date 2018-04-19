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
        println("Server Started")
        while (true) {
            val client = SecureClient(server.accept())
            println("got client: ${client.channel.remoteAddress}")
            val decodedMessage = client.decodeMessage()
            println("Message was: $decodedMessage")

            if (!accessGranted) {
                if (decodedMessage == password) {
                    accessGranted = true
                    println("Access granted")
                    client.writeMessage("ACCESS_GRANTED")
                } else {
                    println("Access denied")
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
                        println("test1")
                        val response = arduino.sendCommand(decodedMessage)
                        println("test2 $response")

                        client.writeMessage(response)
                        println("test3")

                    }
                }
            }
            println("end")
        }

    }
}
