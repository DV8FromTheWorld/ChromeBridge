package net.dv8tion.chromebridge
import org.junit.Assert.*
import org.junit.Test
class BridgeProtocolTest {
    @Test fun proofsBindRoleNonceAndUrl() {
        val key = "0123456789abcdef0123456789abcdef"
        assertEquals(BridgeProtocol.proof(key, "server:nonce"), BridgeProtocol.proof(key, "server:nonce"))
        assertFalse(BridgeProtocol.equal(BridgeProtocol.proof(key, "server:nonce"), BridgeProtocol.proof(key, "client:nonce")))
        assertFalse(BridgeProtocol.equal(BridgeProtocol.proof(key, "client:nonce:https://a.com"), BridgeProtocol.proof(key, "client:nonce:https://b.com")))
        assertFalse(BridgeProtocol.equal(BridgeProtocol.proof(key, "server:nonce"), BridgeProtocol.proof("other", "server:nonce")))
    }
    @Test fun acceptsOrdinaryWebLinks() {
        assertTrue(BridgeProtocol.validUrl("https://example.com/path?q=hello%20world#fragment"))
        assertTrue(BridgeProtocol.validUrl("http://localhost:8080/test"))
    }
    @Test fun rejectsNonWebAndMalformedLinks() {
        listOf("javascript:alert(1)", "file:///etc/passwd", "intent://example.com", "https://", "https://example.com/\nfoo", "https://user:password@example.com", "https://example.com/" + "a".repeat(8192)).forEach { assertFalse(it, BridgeProtocol.validUrl(it)) }
    }
}
