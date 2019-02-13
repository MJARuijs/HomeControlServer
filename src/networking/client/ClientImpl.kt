package networking.client

import networking.nio.NonBlockingClient
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

class ClientImpl(channel: SocketChannel, private val address: String, private val callback: (String, String) -> Unit) : NonBlockingClient(channel) {

    private val readSizeBuffer = ByteBuffer.allocateDirect(Integer.BYTES)

    fun sendCommand(command: String): String {
        write(command)
        return readMessage()
    }

    override fun write(bytes: ByteArray) {
        val buffer = ByteBuffer.allocate(bytes.size + 4)
        buffer.putInt(bytes.size)
        buffer.put(bytes)
        buffer.rewind()

        channel.write(buffer)
    }

    @Throws (ClientException::class)
    override fun read(): ByteArray {

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
        return data.array()
    }

    override fun close() {
        channel.close()
    }

    override fun onRead() {
        callback(readMessage(), address)
    }
}