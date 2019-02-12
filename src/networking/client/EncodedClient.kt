package networking.client

import networking.nio.NonBlockingClient
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets
import java.util.*

abstract class EncodedClient(channel: SocketChannel) : NonBlockingClient(channel) {

    private val writeSizeBuffer = ByteBuffer.allocateDirect(Integer.BYTES)
    private val readSizeBuffer = ByteBuffer.allocateDirect(Integer.BYTES)

    final override fun write(bytes: ByteArray) {

        val buffer = ByteBuffer.wrap(Base64.getEncoder().encode(bytes))

        val bufferSize = buffer.array().size.toString()
        val size = Base64.getEncoder().encode(bufferSize.toByteArray(StandardCharsets.UTF_8))

        // Prepare size buffer
        writeSizeBuffer.clear()
        writeSizeBuffer.put(size)
        writeSizeBuffer.rewind()

        // Write size
        channel.write(writeSizeBuffer)

        buffer.rewind()

        // Write data
        channel.write(buffer)
    }

    @Throws (ClientException::class)
    final override fun read(): ByteArray {

        // Read size
        readSizeBuffer.clear()
        val sizeBytesRead = channel.read(readSizeBuffer)

        if (sizeBytesRead == -1) {
            throw ClientException("Size was too large")
        }

        readSizeBuffer.rewind()

        // Read data
        val size = String(Base64.getDecoder().decode(readSizeBuffer).array(), StandardCharsets.UTF_8).toInt()

        if (size > 1000) {
            println("ERROR: too large: $size")
            throw ClientException("Size was too large")
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

    override fun close() {
        channel.close()
    }
}