package networking.client

import jdk.jshell.spi.ExecutionControlProvider
import networking.nio.NonBlockingClient
import java.lang.Exception
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

class ClientImpl(channel: SocketChannel, private val address: String, private val callback: (String, String) -> Unit) : NonBlockingClient(channel) {

    private val readSizeBuffer = ByteBuffer.allocateDirect(Integer.BYTES)

    var lastMessageReceived = ""

    fun resetLastMessage() {
        lastMessageReceived = ""
    }

    fun getAndResetLastMessage(): String {
        val temp = lastMessageReceived
        lastMessageReceived = ""
        return temp
    }

    fun available(): Boolean {
        return lastMessageReceived != ""
    }

    fun sendCommand(command: String): String {
        write(command)
        return readMessage()
    }

    override fun write(bytes: ByteArray) {
        val buffer = ByteBuffer.allocate(bytes.size + 4)
        buffer.putInt(bytes.size)
        buffer.put(bytes)
        buffer.rewind()
        println("Writing ${String(bytes)}")
        channel.write(buffer)
    }

    @Throws (ClientException::class)
    override fun read(): ByteArray {
        try {
            readSizeBuffer.clear()
            val sizeBytesRead = channel.read(readSizeBuffer)

            if (sizeBytesRead == -1) {
                throw ClientException("Size was too large")
            }

            readSizeBuffer.rewind()

            // Read data
            val size = readSizeBuffer.int

            if (size > 1000) {
                throw ClientException("Size was too large")
            }

            val data = ByteBuffer.allocate(size)
            val bytesRead = channel.read(data)

            if (bytesRead == -1) {
                close()
                throw ClientException("Client was closed")
            }

            data.rewind()

            println("READ STRINGGG ${String(data.array())}")
            return data.array()
        } catch (e: Exception) {
            throw ClientException(e.message!!)
        }
    }

    override fun onRead() {
        lastMessageReceived = readMessage()
        callback(lastMessageReceived, address)
    }

    override fun close() {
        channel.close()
    }
}