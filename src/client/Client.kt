package client

import java.nio.ByteBuffer

interface Client {

    fun write(bytes: ByteArray)

    fun read(): ByteBuffer

    fun close()

}

