package networking.client

import networking.nio.NonBlockingClient
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.util.*

open class EncodedClient(channel: SocketChannel, val address: String, val callback: (String, String) -> Unit) : NonBlockingClient(channel) {

    final override fun write(bytes: ByteArray) {
        try {
            val encodedBytes = Base64.getEncoder().encode(bytes)
            val bufferSize = encodedBytes.size.toString().toByteArray()
            val encodedSize = Base64.getEncoder().encode(bufferSize)
            val buffer = ByteBuffer.allocate(encodedBytes.size + encodedSize.size)
            buffer.put(encodedSize)
            buffer.put(encodedBytes)
            buffer.rewind()
//            println("WRITING:")
//            println(String(encodedBytes))
            channel.write(buffer)
        } catch (e: Exception) {
            e.printStackTrace()
            throw ClientException("Invalid write!")
        }
    }

    @Throws (ClientException::class)
    final override fun read(): ByteArray {

        // Read size
        val readSizeBuffer = ByteBuffer.allocate(Integer.BYTES)
        var sizeBytesRead = channel.read(readSizeBuffer)

        while (sizeBytesRead == 0) {
            sizeBytesRead = channel.read(readSizeBuffer)
        }

        if (sizeBytesRead == -1) {
            throw ClientException("Size was invalid")
        }

        readSizeBuffer.rewind()

        // Read data
        val sizeArray = ByteArray(4)
        var index = 0

        while (readSizeBuffer.hasRemaining()) {
            val b = readSizeBuffer.get()
            sizeArray[index] = b
            index++
        }

        val size = String(Base64.getDecoder().decode(sizeArray)).toInt()

        if (size > 1000) {
            throw ClientException("Size was too large $size")
        }

        val data = ByteBuffer.allocate(size)
        val bytesRead = channel.read(data)

        if (bytesRead == -1) {
            close()
            throw ClientException("Client was closed")
        }

        data.rewind()
        return Base64.getDecoder().decode(data).array()
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