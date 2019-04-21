package networking

import networking.client.SecureClient
import networking.nio.Manager
import networking.nio.NonBlockingServer
import java.nio.channels.SocketChannel

class PhoneServer(address: String, port: Int, private val manager: Manager) : NonBlockingServer(address, port) {

    private val clients = HashMap<String, SecureClient>()

    override fun onAccept(channel: SocketChannel) {
        val channelString = channel.toString()
        val addressStartIndex = channelString.lastIndexOf('/') + 1
        val addressEndIndex = channelString.lastIndexOf(':')

        val address = channelString.substring(addressStartIndex, addressEndIndex)
        println("Accepted Address: $address")

        val client = SecureClient(channel, address, ::onReadCallback)

        val message = client.decodeMessage()

        val idStartIndex = message.indexOf("id=") + 3
        val idEndIndex = message.indexOf(';')

        val id = message.substring(idStartIndex, idEndIndex).toLong()

        // TODO: Perform authentication check here

        client.write("id=$id;ACCESS_GRANTED")

        manager.register(client)
        clients[address] = client
    }

    private fun onReadCallback(message: String, address: String) {
        if (message == "close_connection") {
            clients[address]?.close()
            clients.remove(address)
            return
        }

        val startIndex = message.indexOf("id=") + 3
        val endIndex = message.indexOf(';')

        val id = message.substring(startIndex, endIndex).toInt()

        if (message.contains("get_configuration")) {
            val config = processCommand()
            clients[address]?.write("id=$id;$config")
            return
        }

        val messageContent = message.substring(endIndex + 1)

        if (message.contains("PHONE: ")) {
            if (!clients.containsKey(address)) {
                clients[address] = clients[address] ?: return
            }
            try {
                clients[address]?.write("id=$id;ACCESS_GRANTED")
            } catch (e: Exception) {
                clients[address] = clients[address] ?: return
                clients[address]?.write("id=$id;ACCESS_GRANTED")
            }
            clients.remove(address)
            return
        }

        val result = processCommand(messageContent)
        clients[address]?.write("id=$id;$result")
    }

    private fun processCommand(messageInfo: String = "get_configuration"): String {
        val requestId = RequestQueue.addRequest("ROOM", messageInfo)

        while (!RequestQueue.containsResponse(requestId)) {}

        return RequestQueue.takeResponseIfAvailable(requestId)
    }
}