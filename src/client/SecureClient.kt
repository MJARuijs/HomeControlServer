package client

import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets.UTF_8
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class SecureClient(channel: SocketChannel): EncodedClient(channel) {

    private companion object {
        val asymmetricGenerator: KeyPairGenerator = KeyPairGenerator.getInstance("RSA")
        val symmetricGenerator: KeyGenerator = KeyGenerator.getInstance("AES")

        init {
            asymmetricGenerator.initialize(2048, SecureRandom.getInstance("SHA1PRNG"))
            symmetricGenerator.init(128)
        }
    }

    private val encryptor = Cipher.getInstance("RSA/ECB/PKCS1Padding")
    private val decryptor = Cipher.getInstance("RSA/ECB/PKCS1Padding")
    private val symmetricKey: SecretKey

    init {
        println("1")
        symmetricKey = symmetricGenerator.generateKey()
        println("2")

        val keyPair = asymmetricGenerator.generateKeyPair()
        println("3")

        val clientKey = keyPair.private
        println("4")

        write(keyPair.public.encoded)
        println("5")

        val keyFactory = KeyFactory.getInstance("RSA")
        println("6")

        val serverKey = keyFactory.generatePublic(X509EncodedKeySpec(read().array()))
        println("7")

        encryptor.init(Cipher.PUBLIC_KEY, serverKey)
        println("8")

        decryptor.init(Cipher.PRIVATE_KEY, clientKey)
        println("9")

    }

    fun decodeMessage(): String {
        val message = read().array()
        val key = read().array()

        val decryptedKey = decryptor.doFinal(key)

        val secretKey = SecretKeySpec(decryptedKey, 0, decryptedKey.size, "AES")
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.DECRYPT_MODE, secretKey)

        val decryptedMessage = cipher.doFinal(message)

        return String(decryptedMessage, UTF_8)
    }

    fun writeMessage(message: String) {
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, symmetricKey)
        val messageBytes = cipher.doFinal(message.toByteArray(UTF_8))

        val keyBytes = encryptor.doFinal(symmetricKey.encoded)

        write(messageBytes)
        write(keyBytes)
    }

}