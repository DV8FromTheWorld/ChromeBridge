package net.dv8tion.chromebridge

import java.net.URI

object BridgeProtocol {
    const val PORT = 45678
    const val MAGIC = "ChromeBridge/2"
    fun proof(key: String, message: String): String {
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        mac.init(javax.crypto.spec.SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(message.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }
    fun equal(a: String, b: String) = java.security.MessageDigest.isEqual(a.toByteArray(), b.toByteArray())
    fun validUrl(url: String): Boolean = try {
        val uri = URI(url)
        url.length <= 8192 && !url.any { it <= '\u0020' || it == '\u007f' } &&
            uri.scheme?.lowercase() in listOf("http", "https") && !uri.host.isNullOrBlank() && uri.rawUserInfo == null
    } catch (_: Exception) { false }
}
