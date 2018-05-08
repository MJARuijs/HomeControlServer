package client

import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets

class ArduinoClient(val name: String, private val channel: SocketChannel): Client {

    private val writeSizeBuffer = ByteBuffer.allocateDirect(Integer.BYTES)
    private val readSizeBuffer = ByteBuffer.allocateDirect(Integer.BYTES)

    override fun write(bytes: ByteArray) {

        // Prepare size buffer
        writeSizeBuffer.clear()
        writeSizeBuffer.putInt(bytes.size)
        writeSizeBuffer.rewind()

        // Write size
        channel.write(writeSizeBuffer)

        val buffer = ByteBuffer.wrap(bytes)

        buffer.rewind()

        // Write data
        channel.write(buffer)
    }

    override fun read(): ByteBuffer {

        // Read size
        readSizeBuffer.clear()
        val sizeBytesRead = channel.read(readSizeBuffer)

        if (sizeBytesRead == -1) {
            close()
            throw ClientException("client.Client was closed")
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
            throw ClientException("client.Client was closed")
        }

        data.rewind()
        return data
    }

    fun writeMessage(message: String) {
        val bytes = message.toByteArray(StandardCharsets.UTF_8)
        write(bytes)
    }

    fun readMessage(): String {

        return try {
            val buffer = read()
            val bytes = buffer.array()
            String(bytes, StandardCharsets.UTF_8)
        } catch (e: ClientException) {
            "ERROR"
        }
    }

    fun sendCommand(message:String): String {
        writeMessage(message)
        val read = readMessage()
        return if (read == "ERROR") {
            sendCommand(message)
        } else {
            read
        }
    }

    override fun close() {
        channel.close()
    }

}