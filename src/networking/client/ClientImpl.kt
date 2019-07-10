package networking.client

import networking.nio.NonBlockingClient
import java.lang.Exception
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

class ClientImpl(channel: SocketChannel, private val address: String, private val callback: (String, String) -> Unit) : NonBlockingClient(channel) {

    private val readSizeBuffer = ByteBuffer.allocateDirect(Integer.BYTES)

    override fun write(bytes: ByteArray) {
        try {
            val buffer = ByteBuffer.allocate(bytes.size + 4)
            buffer.putInt(bytes.size)
            buffer.put(bytes)
            buffer.rewind()
            println("Writing ${String(bytes)} to $address")
            channel.write(buffer)
        } catch (e: Exception) {
            e.printStackTrace()
            throw ClientException("Invalid write")
        }
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
            return data.array()
        } catch (e: Exception) {
            throw ClientException("Invalid Read")
        }
    }

    override fun onRead() {
        val message = readMessage()

        Thread {
            callback(message, address)
        }.start()
    }

    override fun close() {
        super.close()
        channel.close()
    }
}