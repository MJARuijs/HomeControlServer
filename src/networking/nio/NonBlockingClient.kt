package networking.nio

import networking.client.Client
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel

abstract class NonBlockingClient(internal val channel: SocketChannel) : Client, Registrable {

    init {
        channel.configureBlocking(false)
    }

    override fun register(selector: Selector) {
        channel.register(selector, SelectionKey.OP_READ, this)
    }

    abstract fun onRead()

}