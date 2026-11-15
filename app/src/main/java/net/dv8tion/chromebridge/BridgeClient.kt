package net.dv8tion.chromebridge

import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID

object BridgeClient {
    data class Result(val ok: Boolean, val message: String)
    // Only connection establishment is retried automatically. Never resend a request
    // after sending its URL: the response may have been lost after Chrome launched.
    fun send(url: String, key: String): Result {
        if (!BridgeProtocol.validUrl(url)) return Result(false, "Only valid HTTP(S) links are supported.")
        if (key.isEmpty()) return Result(false, "Pair the Work app with Personal Chrome Bridge first.")
        var socket: Socket? = null
        repeat(3) { attempt ->
            if (socket == null) {
                val candidate = Socket()
                try {
                    candidate.connect(InetSocketAddress("127.0.0.1", BridgeProtocol.PORT), 700)
                    socket = candidate
                } catch (_: Exception) {
                    candidate.close()
                    if (attempt < 2) Thread.sleep(500)
                }
            }
        }
        val connected = socket ?: return Result(false, "Personal listener is offline. Android may be restarting it, it may have been stopped, or a profile/VPN may block the connection.")
        var submitted = false
        return try {
            connected.use {
                it.soTimeout = 3500
                val output = DataOutputStream(it.getOutputStream())
                val input = DataInputStream(it.getInputStream())
                val nonce = UUID.randomUUID().toString()
                output.writeUTF(BridgeProtocol.MAGIC)
                output.writeUTF(nonce)
                output.flush()
                if (!BridgeProtocol.equal(input.readUTF(), BridgeProtocol.proof(key, "server:$nonce")))
                    return Result(false, "Listener could not be authenticated. Check the pairing codes in both profiles.")
                submitted = true
                output.writeUTF(url)
                output.writeUTF(BridgeProtocol.proof(key, "client:$nonce:$url"))
                output.flush()
                val reply = input.readUTF()
                Result(reply == "OK", reply)
            }
        } catch (_: Exception) {
            Result(false, if (submitted) "No acknowledgement received. Chrome may already have opened the link. Check before retrying."
                else "Could not authenticate the listener. Check that Personal Chrome Bridge is running and the pairing codes match.")
        }
    }
}
