package networking

import networking.client.ClientImpl
import networking.nio.Manager
import networking.nio.NonBlockingServer
import util.Logger
import java.io.FileWriter
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

class RoomServer(address: String, port: Int, private val manager: Manager, private val knownModules: ArrayList<String>) : NonBlockingServer(address, port) {

    private val clients = HashMap<String, Pair<String, ClientImpl>>()
    private val configuration = ArrayList<String>()
    private var requiredModuleConfigs = HashSet<String>()

    init {
        Thread {
            while (true) {
                try {
                    val request = RequestQueue.takeRequestIfAvailable("ROOM")

                    if (request != null) {
                        if (request.second.contains("configuration")) {
                            val config = getConfiguration(request.second)
                            RequestQueue.finishRequest(request.first, config)
                        } else {
                            val result = processCommand(request.second)
                            RequestQueue.finishRequest(request.first, result)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }.start()
    }

    fun init() {
        knownModules.forEach { client ->
            try {
                val channel = SocketChannel.open()
                channel.connect(InetSocketAddress(client, 4443))
                val bytes = "SERVER_ADDRESS:$address".toByteArray()
                val buffer = ByteBuffer.allocate(bytes.size + 4)
                buffer.putInt(bytes.size)
                buffer.put(bytes)
                buffer.rewind()
                channel.write(buffer)
            } catch (e: Exception) {
                Logger.warn("FAILED CONNECTION WITH $client")
            }
        }
    }

    override fun onAccept(channel: SocketChannel) {
        val channelString = channel.toString()
        val addressStartIndex = channelString.lastIndexOf('/') + 1
        val endIndex = channelString.lastIndexOf(':')

        val address = channelString.substring(addressStartIndex, endIndex)
        Logger.debug("Accepted Address: $address")

        val client = ClientImpl(channel, address, ::onReadCallback)

        if (!knownModules.contains(address) && !clients.containsKey(address)) {
            addToFile(address)
        }

        manager.register(client)
        clients[address] = Pair("", client)
    }

    private fun onReadCallback(message: String, address: String) {
        if (message.startsWith("PI")) {
            val roomStartIndex = message.indexOf(':') + 1
            val room = message.substring(roomStartIndex, message.length).trim().toLowerCase()
            clients[address] = Pair(room, clients[address]?.second ?: return)
        }
        if (requiredModuleConfigs.contains(address)) {
            requiredModuleConfigs.remove(address)
            configuration += message
        }
    }

    private fun processCommand(command: String): String {
        configuration.clear()
        requiredModuleConfigs.clear()
        requiredModuleConfigs.addAll(clients.keys)

        val messageInfo = command.split('|')

        val room = messageInfo[0].trim().toLowerCase()
        val mcuType = messageInfo[1].trim().toLowerCase()
        val data = messageInfo[2]

        clients.forEach { (_, roomClient) ->
            if (roomClient.first == room) {
                roomClient.second.write("$mcuType|$data")
            } else {
                roomClient.second.write("get_configuration")
            }
        }

        while (requiredModuleConfigs.isNotEmpty()) {
            Thread.sleep(1)
        }

        return configuration.joinToString("|", "", "", -1, "", null)
    }

    private fun getConfiguration(command: String): String {
        configuration.clear()
        requiredModuleConfigs.clear()
        requiredModuleConfigs.addAll(clients.keys)

        clients.forEach { client ->
            client.value.second.write(command)
        }

        while (requiredModuleConfigs.isNotEmpty()) {
            Thread.sleep(1)
        }

        return configuration.joinToString("|", "", "", -1, "", null)
    }

    private fun addToFile(connection: String) {
        val printWriter = PrintWriter(FileWriter("connections.txt", true))
        printWriter.println(connection)
        printWriter.close()
    }
}