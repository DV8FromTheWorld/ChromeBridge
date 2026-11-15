package net.dv8tion.chromebridge

import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import org.junit.Assert.*
import org.junit.Test

class BridgeClientTest {
    private val key = "0123456789abcdef0123456789abcdef"
    private val url = "https://example.com/test"
    private fun withListener(serverAction: (DataInputStream, DataOutputStream) -> Unit, clientAction: () -> Unit) {
        val error = AtomicReference<Throwable?>()
        ServerSocket().use { server ->
            server.reuseAddress = true
            server.bind(InetSocketAddress("127.0.0.1", BridgeProtocol.PORT))
            server.soTimeout = 5000
            val worker = thread {
                try { server.accept().use { socket ->
                    socket.soTimeout = 5000
                    serverAction(DataInputStream(socket.getInputStream()), DataOutputStream(socket.getOutputStream()))
                } } catch (t: Throwable) { error.set(t) }
            }
            clientAction()
            worker.join(6000)
            assertFalse("server thread finished", worker.isAlive)
            error.get()?.let { throw AssertionError("server failure", it) }
        }
    }
    @Test fun authenticatesBeforeSendingUrl() = withListener({ input, output ->
        assertEquals(BridgeProtocol.MAGIC, input.readUTF())
        val nonce = input.readUTF()
        output.writeUTF(BridgeProtocol.proof(key, "server:$nonce")); output.flush()
        assertEquals(url, input.readUTF())
        assertEquals(BridgeProtocol.proof(key, "client:$nonce:$url"), input.readUTF())
        output.writeUTF("OK"); output.flush()
    }, { assertTrue(BridgeClient.send(url, key).ok) })
    @Test fun rejectsImpersonatorWithoutSendingUrl() = withListener({ input, output ->
        input.readUTF(); input.readUTF()
        output.writeUTF("wrong proof"); output.flush()
        assertEquals("must not disclose URL", -1, input.read())
    }, { assertFalse(BridgeClient.send(url, key).ok) })
    @Test fun missingAcknowledgementDoesNotResend() = withListener({ input, output ->
        input.readUTF(); val nonce = input.readUTF()
        output.writeUTF(BridgeProtocol.proof(key, "server:$nonce")); output.flush()
        assertEquals(url, input.readUTF()); input.readUTF()
        // Closing the socket simulates losing the reply after Chrome launched.
    }, {
        val result = BridgeClient.send(url, key)
        assertFalse(result.ok)
        assertTrue(result.message.contains("may already"))
    })
    @Test fun refusesInvalidInputWithoutConnecting() {
        assertFalse(BridgeClient.send("file:///etc/passwd", key).ok)
        assertFalse(BridgeClient.send(url, "").ok)
    }
}
