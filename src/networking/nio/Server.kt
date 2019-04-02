package networking.nio

import networking.client.ClientImpl
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

class Server(port: Int, private val manager: Manager, private val knownRoomModules: ArrayList<String>) : NonBlockingServer(port) {

    private val clients = HashMap<String, ClientImpl>()
    private val roomClients = HashMap<String, Pair<String, ClientImpl>>()
    private val phoneClients = HashMap<String, ClientImpl>()
    private val configuration = ArrayList<String>()

    fun init() {
        knownRoomModules.forEach { client ->
            println("Client: $client")
            try {
                val channel = SocketChannel.open()
                channel.connect(InetSocketAddress(client, 4441))
                val bytes = "SERVER_ADDRESS:192.168.178.18".toByteArray()
                val buffer = ByteBuffer.allocate(bytes.size + 4)
                buffer.putInt(bytes.size)
                buffer.put(bytes)
                buffer.rewind()
                channel.write(buffer)
                channel.close()
            } catch (e: Exception) {
                e.printStackTrace()
                println("FAILED CONNECTION WITH $client")
            }
        }
    }

    override fun onAccept(channel: SocketChannel) {
        val channelString = channel.toString()
        val startIndex = channelString.lastIndexOf('/') + 1
        val endIndex = channelString.lastIndexOf(':')

        val address = channelString.substring(startIndex, endIndex)
        println("Accepted Address: $address")

        val client = ClientImpl(channel, address, ::onReadCallback)
        manager.register(client)
        clients[address] = client
    }

    private fun onReadCallback(message: String, address: String) {
        if (phoneClients.containsKey(address)) {
            if (message == "get_configuration") {
                processConfigurationRequest(address)
                return
            }

            if (message == "PHONE") {
                phoneClients[address]?.write("ACCESS_GRANTED")
            }

            val messageInfo = message.split('|')

            if (messageInfo.size != 3) {
                println("Invalid message! : $message")
                return
            }

            val room = messageInfo[0].trim().toLowerCase()
            val mcuType = messageInfo[1].trim().toLowerCase()
            val data = messageInfo[2]

            roomClients.forEach { _, roomClient ->
                if (roomClient.first == room) {
                    roomClient.second.write("$mcuType|$data")
                }
            }

            phoneClients[address]!!.write("SUCCESS")
        } else if (clients.containsKey(address)) {
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
                println("REGISTERED NEW PHONE: $address")
            }
        }
        println("MESSAGE $message. FROM $address")
    }

    private fun processConfigurationRequest(address: String) {
        var configuration = ""
        roomClients.forEach { clientData ->
            val client = clientData.value.second

//            client.lastMessageReceived = ""
            client.write("get_configuration")

//            while (!client.available()) {
//                println("Wa")
//            }
//            configuration += client.getAndResetLastMessage()

            println("Configuration: $configuration")
        }

        phoneClients[address]?.write(configuration)
    }

    private fun onConfigurationsReceived() {

    }

}