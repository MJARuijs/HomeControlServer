import client.SecureClient

object Main {

    private const val password = "9"

    private var roomModule = RoomModule("192.168.178.24", 4444)
    private var accessGranted = false
    private var connectionAttempts = 0

    @JvmStatic
    fun main(args: Array<String>) {

        val server = Server(4443)

        sendToRoom("NotificationColor: r=24432, g=254, b=254")

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
            if (connectionAttempts < 10) {
                println("Attempts: $connectionAttempts")
                connectionAttempts++
                sendToRoom(message)
            } else {
                "Error"
            }
        } else {
            connectionAttempts = 0
            response
        }
    }
}
