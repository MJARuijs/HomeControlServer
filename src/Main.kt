import client.SecureClient
import java.net.InetSocketAddress
import java.nio.channels.SocketChannel

object Main {

    private const val password = "9"

    private var roomModule = SecureClient(SocketChannel.open(InetSocketAddress("192.168.178.19", 4444)))
    private var accessGranted = false

    @JvmStatic
    fun main(args: Array<String>) {

        val server = Server(4443)
        println("Server started")
        while (true) {

            val client = SecureClient(server.accept())
            val decodedMessage = client.readMessage()

            println("Message: $decodedMessage")

            if (!accessGranted) {
                when {
                    decodedMessage.startsWith("NotificationColor: ") -> sendToRoom(decodedMessage)
                    decodedMessage == password -> {
                        accessGranted = true
                        client.writeMessage("ACCESS_GRANTED")
                    }
                    else -> client.writeMessage("ACCESS_DENIED")
                }
            } else {
                when (decodedMessage) {
                    "close_connection" -> {
                        accessGranted = false
                        client.writeMessage("CONNECTION_CLOSED")
                        client.close()
                    }
                    else -> {
                        val response = sendToRoom(decodedMessage)
                        client.writeMessage(response)
                    }
                }
            }
        }
    }

    private fun sendToRoom(message: String): String {
        roomModule.writeMessage(message)
        val response = roomModule.readMessage()
        println("RESPONSE $response")
        return if (response == "ERROR") {
            sendToRoom(message)
        } else {
            response
        }
    }
}
