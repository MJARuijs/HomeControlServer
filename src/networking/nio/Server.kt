package networking.nio

import networking.client.ClientImpl
import java.nio.channels.SocketChannel

class Server(port: Int, private val manager: Manager) : NonBlockingServer(port) {

    private val clients = HashMap<String, ClientImpl>()
    private val roomClients = HashMap<String, Pair<String, ClientImpl>>()
    private val phoneClients = HashMap<String, ClientImpl>()

    override fun onAccept(channel: SocketChannel) {
        val channelString = channel.toString()
        val startIndex = channelString.lastIndexOf('/') + 1
        val endIndex = channelString.lastIndexOf(':')

        val address = channelString.substring(startIndex, endIndex)
        println(address)

        val client = ClientImpl(channel, address, ::onReadCallback)
        manager.register(client)
        clients[address] = client
    }

    private fun onReadCallback(message: String, address: String) {
        if (clients.containsKey(address)) {

            if (message.startsWith("PI")) {
                val startIndex = message.indexOf(':') + 1
                val room = message.substring(startIndex, message.length).trim().toLowerCase()
                roomClients[address] = Pair(room, clients[address] ?: return)
                clients.remove(address)
            }

            if (message == "PHONE") {
                phoneClients[address] = clients[address] ?: return
                phoneClients[address]?.write("ACCESS_GRANTED")
                clients.remove(address)
            }
        } else if (phoneClients.containsKey(address)) {

            if (message == "get_configuration") {
                var configuration = ""
                roomClients.forEach { client ->
                    configuration += client.value.second.sendCommand("get_configuration")
                }

                println(configuration)
                phoneClients[address]?.write(configuration)
                return
            }

            val messageInfo = message.split('|')

            if (messageInfo.size != 3) {
                println(message)
                return
            }
            val room = messageInfo[0].trim().toLowerCase()
            val mcuType = messageInfo[1].trim().toLowerCase()
            val data = messageInfo[2]

            roomClients.forEach { _, roomClient ->
                println(room)
                println(roomClient.first)
                if (roomClient.first == room) {
                    roomClient.second.write("$mcuType|$data")
                }
            }

            phoneClients[address]!!.write("SUCCESS")
        }

        println("MESSAGE $message. FROM $address")
    }

}