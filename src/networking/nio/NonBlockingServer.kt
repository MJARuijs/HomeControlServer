package networking.nio

import java.net.InetSocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel

abstract class NonBlockingServer(port: Int) : Registrable {

    private val channel = ServerSocketChannel.open()!!

    init {
        val address = InetSocketAddress("192.168.178.18", port)
        channel.bind(address)
        channel.configureBlocking(false)
    }

    final override fun register(selector: Selector) {
        channel.register(selector, SelectionKey.OP_ACCEPT, this)
    }

    fun accept(): SocketChannel {
        return channel.accept()
    }

    abstract fun onAccept(channel: SocketChannel)

}