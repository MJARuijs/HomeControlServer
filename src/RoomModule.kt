import client.SecureClient
import java.net.InetSocketAddress
import java.nio.channels.SocketChannel

class RoomModule(address: String, port: Int) : SecureClient(SocketChannel.open(InetSocketAddress(address, port)))