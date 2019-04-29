package networking.nio

import networking.client.ClientImpl
import networking.client.SecureClient
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

@Deprecated("")
class Server(address: String, port: Int, private val manager: Manager, private val knownRoomModules: ArrayList<String>) : NonBlockingServer(address, port) {

    private val clients = HashMap<String, SecureClient>()
    private val roomClients = HashMap<String, Pair<String, ClientImpl>>()
    private val phoneClients = HashMap<String, SecureClient>()
    private val configuration = ArrayList<String>()

    private var requiredModuleConfigs = HashSet<String>()

    fun init() {
        knownRoomModules.forEach { client ->
            println("Client: $client")
            try {
                val channel = SocketChannel.open()
                channel.connect(InetSocketAddress(client, 4441))
                val bytes = "SERVER_ADDRESS:$address".toByteArray()
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

        val client = SecureClient(channel, address, ::onReadCallback)
        manager.register(client)
        clients[address] = client
    }

    private fun onReadCallback(message: String, address: String) {
        println("MESSAGE $message. FROM $address")
        if (phoneClients.containsKey(address)) {
            if (message == "close_connection") {
                phoneClients[address]?.close()
                phoneClients.remove(address)
                return
            }

            val startIndex = message.indexOf("id=") + 3
            val endIndex = message.indexOf(';')

            val id = message.substring(startIndex, endIndex).toInt()

            if (message.contains("get_configuration")) {
                processConfigurationRequest(address, id)
                return
            }

            val messageContent = message.substring(endIndex + 1)
            val messageInfo = messageContent.split('|')

            if (messageInfo.size != 3 && !message.contains("PHONE")) {
                println("Invalid message! : $message")
                return
            }

            if (message.contains("PHONE: ")) {
                if (!phoneClients.containsKey(address)) {
                    phoneClients[address] = clients[address] ?: return
                }
                try {
                    phoneClients[address]?.write("id=$id;ACCESS_GRANTED")
                } catch (e: Exception) {
                    phoneClients[address] = clients[address] ?: return
                    phoneClients[address]?.write("id=$id;ACCESS_GRANTED")
                }
                clients.remove(address)
                return
            }

            processCommand(messageInfo, address, id)
        } else if (clients.containsKey(address)) {
            if (message.startsWith("PI")) {
                val startIndex = message.indexOf(':') + 1
                val room = message.substring(startIndex, message.length).trim().toLowerCase()
//                roomClients[address] = Pair(room, clients[address] ?: return)
                clients.remove(address)
            }

            if (message.contains("PHONE: ")) {
                val startIndex = message.indexOf("id=") + 3
                val endIndex = message.indexOf(';')

                val id = message.substring(startIndex, endIndex).toLong()

                phoneClients[address] = clients[address] ?: return
                phoneClients[address]?.write("id=$id;ACCESS_GRANTED")
                clients.remove(address)
                println("REGISTERED NEW PHONE: $address")
            } else if (message.contains("led_strip")) {
                val startIndex = message.indexOf("id=") + 3
                val endIndex = message.indexOf(';')

                val id = message.substring(startIndex, endIndex).toInt()
                val messageContent = message.substring(endIndex + 1)

                val messageInfo = messageContent.split('|')

                if (messageInfo.size != 3) {
                    println("Invalid message! : $message")
                    return
                }

                processCommand(messageInfo, address, id)
            }
        } else if (roomClients.containsKey(address)) {
            if (requiredModuleConfigs.contains(address)) {
                requiredModuleConfigs.remove(address)
                configuration.add(message)
            }
        }
    }

    private fun processCommand(messageInfo: List<String>, address: String, messageId: Int) {
        configuration.clear()
        requiredModuleConfigs.clear()
        requiredModuleConfigs.addAll(roomClients.keys)

        val room = messageInfo[0].trim().toLowerCase()
        val mcuType = messageInfo[1].trim().toLowerCase()
        val data = messageInfo[2]

        roomClients.forEach { (_, roomClient) ->
            if (roomClient.first == room) {
                roomClient.second.write("$mcuType|$data")
            } else {
                roomClient.second.write("get_configuration")
            }
        }

        while (requiredModuleConfigs.isNotEmpty()) {
            Thread.sleep(1)
        }

        val config = configuration.joinToString("|", "", "", -1, "", null)
        phoneClients[address]?.write("id=$messageId;$config")
    }

    private fun processConfigurationRequest(address: String, messageId: Int) {
        configuration.clear()
        requiredModuleConfigs.clear()
        requiredModuleConfigs.addAll(roomClients.keys)

        roomClients.forEach { client ->
            client.value.second.write("get_configuration")
        }

        while (requiredModuleConfigs.isNotEmpty()) {
            Thread.sleep(1)
        }

        val config = configuration.joinToString("|", "", "", -1, "", null)
        phoneClients[address]?.write("id=$messageId;$config")
    }

}