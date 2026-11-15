package net.dv8tion.chromebridge

import android.app.*
import android.content.Intent
import android.net.Uri
import android.os.*
import android.provider.Settings
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class BridgeService : Service() {
    companion object {
        @Volatile private var activeServer: ServerSocket? = null
        val isListening: Boolean get() = activeServer?.let { it.isBound && !it.isClosed } == true
    }
    private var server: ServerSocket? = null
    @Volatile private var running = false
    private val main = Handler(Looper.getMainLooper())
    override fun onCreate() {
        super.onCreate()
        if (BridgeState.isWork(this) || !BridgeState.enabled(this) || !Settings.canDrawOverlays(this)) {
            stopSelf(); return
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("bridge", "Chrome Bridge", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1, Intent(this, BridgeService::class.java).setAction("STOP"), PendingIntent.FLAG_IMMUTABLE)
        startForeground(1, Notification.Builder(this, "bridge")
            .setContentTitle("Chrome Bridge is listening")
            .setContentText("Work links open in Personal Chrome. Tap to manage.")
            .setSmallIcon(android.R.drawable.ic_menu_share).setContentIntent(open).setOngoing(true)
            .addAction(Notification.Action.Builder(null, "Stop", stop).build()).build())
        running = true
        thread(name = "bridge-listener") {
            try {
                val listener = ServerSocket().apply { reuseAddress = true; bind(InetSocketAddress("127.0.0.1", BridgeProtocol.PORT), 5) }
                server = listener
                activeServer = listener
                if (!running) { listener.close(); return@thread }
                while (running) {
                    listener.accept().use { socket ->
                        try {
                            socket.soTimeout = 1500
                            val input = DataInputStream(socket.getInputStream())
                            val magic = input.readUTF()
                            val nonce = input.readUTF()
                            val expected = getSharedPreferences("bridge", MODE_PRIVATE).getString("key", "").orEmpty()
                            if (magic != BridgeProtocol.MAGIC || expected.isEmpty() || nonce.length != 36) throw IllegalArgumentException("Invalid handshake")
                            val output = DataOutputStream(socket.getOutputStream())
                            output.writeUTF(BridgeProtocol.proof(expected, "server:$nonce"))
                            output.flush()
                            val url = input.readUTF()
                            val proof = input.readUTF()
                            val result = if (!BridgeProtocol.equal(proof, BridgeProtocol.proof(expected, "client:$nonce:$url"))) "Pairing code does not match."
                                else if (!BridgeProtocol.validUrl(url)) "Only valid HTTP(S) links are supported."
                                else {
                                    getSharedPreferences("bridge", MODE_PRIVATE).edit().putLong("last_received", System.currentTimeMillis()).apply()
                                    openChrome(url)
                                }
                            DataOutputStream(socket.getOutputStream()).apply { writeUTF(result); flush() }
                        } catch (_: Exception) { /* Malformed client must not kill the listener. */ }
                    }
                }
            } catch (_: Exception) {
                if (running) main.post {
                    android.widget.Toast.makeText(this, "Listener stopped: local port unavailable. Restart Chrome Bridge.", android.widget.Toast.LENGTH_LONG).show()
                    stopSelf()
                }
            }
        }
    }
    private fun openChrome(url: String): String {
        val done = CountDownLatch(1)
        var result = "Chrome did not respond."
        main.post {
            result = try {
                if (!Settings.canDrawOverlays(this)) "Grant Display over other apps in Personal Chrome Bridge."
                else {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).setPackage("com.android.chrome").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    "OK"
                }
            } catch (_: Exception) { "Personal Chrome is unavailable. Install or enable Chrome." }
            done.countDown()
        }
        done.await(2, TimeUnit.SECONDS)
        return result
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            BridgeState.setEnabled(this, false)
            stopSelf()
            return START_NOT_STICKY
        }
        if (!running || !BridgeState.enabled(this) || BridgeState.isWork(this)) { stopSelf(); return START_NOT_STICKY }
        return START_STICKY
    }
    override fun onDestroy() {
        running = false
        try { server?.close() } catch (_: Exception) {}
        if (activeServer === server) activeServer = null
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
