package com.onlinepicketline.opl.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.onlinepicketline.opl.R
import com.onlinepicketline.opl.data.model.BlockedRequest
import com.onlinepicketline.opl.data.model.BlocklistEntry
import com.onlinepicketline.opl.data.model.UserAction
import com.onlinepicketline.opl.data.repository.OplRepository
import com.onlinepicketline.opl.ui.BlockPageActivity
import com.onlinepicketline.opl.ui.MainActivity
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Local VPN service that intercepts DNS traffic to detect and
 * optionally block connections to employers with active labor actions.
 *
 * Design:
 * - Establishes a local VPN tunnel
 * - Intercepts DNS queries to extract requested domain names
 * - Checks domains against the cached blocklist
 * - Shows a notification when a blocked domain is detected
 * - User can choose to proceed or respect the picket line
 * - Non-blocked traffic passes through unchanged
 */
class OplVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val isRunning = AtomicBoolean(false)
    private var vpnThread: Thread? = null
    private lateinit var repository: OplRepository

    // Track blocked requests and user decisions
    private val blockedRequests = ConcurrentHashMap<String, BlockedRequest>()
    private val allowedDomains = ConcurrentHashMap.newKeySet<String>()

    companion object {
        private const val TAG = "OplVpnService"
        const val CHANNEL_ID = "opl_vpn_channel"
        const val ALERT_CHANNEL_ID = "opl_alert_channel"
        const val VPN_NOTIFICATION_ID = 1
        const val ALERT_NOTIFICATION_BASE_ID = 100

        const val ACTION_START = "com.onlinepicketline.opl.START_VPN"
        const val ACTION_STOP = "com.onlinepicketline.opl.STOP_VPN"
        const val ACTION_ALLOW_DOMAIN = "com.onlinepicketline.opl.ALLOW_DOMAIN"
        const val ACTION_BLOCK_DOMAIN = "com.onlinepicketline.opl.BLOCK_DOMAIN"
        const val EXTRA_DOMAIN = "extra_domain"

        // VPN tunnel configuration
        private const val VPN_ADDRESS = "10.0.0.2"
        private const val VPN_ROUTE = "0.0.0.0"
        private const val DNS_SERVER = "8.8.8.8"
        private const val MTU = 1500

        @Volatile
        var instance: OplVpnService? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        repository = OplRepository(applicationContext)
        instance = this
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopVpn()
                return START_NOT_STICKY
            }
            ACTION_ALLOW_DOMAIN -> {
                val domain = intent.getStringExtra(EXTRA_DOMAIN)
                if (domain != null) {
                    allowDomain(domain)
                }
                return START_STICKY
            }
            ACTION_BLOCK_DOMAIN -> {
                val domain = intent.getStringExtra(EXTRA_DOMAIN)
                if (domain != null) {
                    blockDomain(domain)
                }
                return START_STICKY
            }
            else -> {
                startVpn()
                return START_STICKY
            }
        }
    }

    private fun startVpn() {
        if (isRunning.getAndSet(true)) return

        startForeground(VPN_NOTIFICATION_ID, buildVpnNotification())

        try {
            vpnInterface = Builder()
                .setSession("Online Picket Line")
                .addAddress(VPN_ADDRESS, 32)
                .addRoute(VPN_ROUTE, 0)
                .addDnsServer(DNS_SERVER)
                .setMtu(MTU)
                .setBlocking(true)
                .establish()

            if (vpnInterface == null) {
                Log.e(TAG, "Failed to establish VPN interface")
                isRunning.set(false)
                stopSelf()
                return
            }

            vpnThread = Thread(::runVpnLoop, "OPL-VPN").apply { start() }
            Log.i(TAG, "VPN started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start VPN", e)
            isRunning.set(false)
            stopSelf()
        }
    }

    /**
     * Main VPN packet processing loop.
     * Reads packets from the VPN interface, inspects DNS queries,
     * and checks against the blocklist.
     */
    private fun runVpnLoop() {
        val fd = vpnInterface ?: return
        val input = FileInputStream(fd.fileDescriptor)
        val output = FileOutputStream(fd.fileDescriptor)
        val packet = ByteBuffer.allocate(MTU)

        try {
            while (isRunning.get()) {
                packet.clear()
                val length = input.read(packet.array())
                if (length <= 0) {
                    Thread.sleep(50)
                    continue
                }
                packet.limit(length)

                // Parse the IP packet to extract DNS queries
                val dnsQuery = parseDnsQuery(packet.array(), length)
                if (dnsQuery != null) {
                    val blocked = checkDomain(dnsQuery)
                    if (blocked != null && !allowedDomains.contains(dnsQuery)) {
                        showBlockNotification(dnsQuery, blocked)
                        // Write the packet through anyway — we notify, not hard-block
                        // Users choose via notification
                    }
                }

                // Forward the packet regardless (notify-only model)
                output.write(packet.array(), 0, length)
            }
        } catch (e: Exception) {
            if (isRunning.get()) {
                Log.e(TAG, "VPN loop error", e)
            }
        } finally {
            input.close()
            output.close()
        }
    }

    /**
     * Parse a DNS query from a UDP packet embedded in an IP packet.
     * Returns the queried domain name, or null if not a DNS query.
     */
    internal fun parseDnsQuery(packet: ByteArray, length: Int): String? {
        if (length < 28) return null // Too short for IP + UDP + DNS

        // Check IP version (must be IPv4)
        val version = (packet[0].toInt() and 0xF0) shr 4
        if (version != 4) return null

        val headerLength = (packet[0].toInt() and 0x0F) * 4
        if (length < headerLength + 8) return null

        // Check protocol (must be UDP = 17)
        val protocol = packet[9].toInt() and 0xFF
        if (protocol != 17) return null

        // Check destination port (must be 53 for DNS)
        val dstPort = ((packet[headerLength + 2].toInt() and 0xFF) shl 8) or
                (packet[headerLength + 3].toInt() and 0xFF)
        if (dstPort != 53) return null

        // Parse DNS query — skip UDP header (8 bytes) + DNS header (12 bytes)
        val dnsStart = headerLength + 8
        val questionStart = dnsStart + 12

        if (questionStart >= length) return null

        return extractDomainName(packet, questionStart, length)
    }

    /**
     * Extract a domain name from DNS question section.
     */
    internal fun extractDomainName(packet: ByteArray, offset: Int, length: Int): String? {
        val parts = mutableListOf<String>()
        var pos = offset

        while (pos < length) {
            val labelLength = packet[pos].toInt() and 0xFF
            if (labelLength == 0) break
            if (pos + labelLength + 1 > length) return null

            val label = String(packet, pos + 1, labelLength, Charsets.US_ASCII)
            parts.add(label)
            pos += labelLength + 1
        }

        return if (parts.isNotEmpty()) parts.joinToString(".").lowercase() else null
    }

    /**
     * Check if a domain is in the blocklist.
     */
    private fun checkDomain(domain: String): BlocklistEntry? {
        return repository.findBlockedUrl(domain)
    }

    /**
     * Try to identify which app is making the network request.
     */
    private fun getRequestingApp(uid: Int): String? {
        return try {
            val pm = packageManager
            val packages = pm.getPackagesForUid(uid)
            packages?.firstOrNull()?.let { pkg ->
                try {
                    pm.getApplicationLabel(
                        pm.getApplicationInfo(pkg, 0)
                    ).toString()
                } catch (_: PackageManager.NameNotFoundException) { pkg }
            }
        } catch (_: Exception) { null }
    }

    /**
     * Show a notification alerting the user about a blocked domain.
     */
    private fun showBlockNotification(domain: String, entry: BlocklistEntry) {
        // Avoid duplicate notifications for the same domain
        if (blockedRequests.containsKey(domain)) return

        val request = BlockedRequest(
            url = domain,
            employer = entry.employer,
            actionType = entry.actionType,
            appPackage = null
        )
        blockedRequests[domain] = request

        val notificationId = ALERT_NOTIFICATION_BASE_ID + domain.hashCode().let {
            if (it < 0) -it else it
        } % 10000

        // Intent to open the block page with details
        val blockPageIntent = Intent(this, BlockPageActivity::class.java).apply {
            putExtra("domain", domain)
            putExtra("employer", entry.employer)
            putExtra("actionType", entry.actionType)
            putExtra("actionId", entry.actionId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val blockPagePending = PendingIntent.getActivity(
            this, notificationId, blockPageIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Allow action
        val allowIntent = Intent(this, OplVpnService::class.java).apply {
            action = ACTION_ALLOW_DOMAIN
            putExtra(EXTRA_DOMAIN, domain)
        }
        val allowPending = PendingIntent.getService(
            this, notificationId + 50000, allowIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("⚠️ Active Labor Action: ${entry.employer}")
            .setContentText("${entry.actionType.replaceFirstChar { it.uppercase() }} in progress — tap for details")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Workers at ${entry.employer} have an active ${entry.actionType}. " +
                        "Domain $domain is associated with this employer. " +
                        "Tap to learn more about the action."))
            .setContentIntent(blockPagePending)
            .addAction(0, "Proceed Anyway", allowPending)
            .addAction(0, "View Details", blockPagePending)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .build()

        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(notificationId, notification)
    }

    private fun allowDomain(domain: String) {
        allowedDomains.add(domain)
        blockedRequests[domain]?.let {
            blockedRequests[domain] = it.copy(userAction = UserAction.ALLOWED)
        }
        val notificationId = ALERT_NOTIFICATION_BASE_ID + domain.hashCode().let {
            if (it < 0) -it else it
        } % 10000
        getSystemService(NotificationManager::class.java).cancel(notificationId)
        Log.i(TAG, "User allowed domain: $domain")
    }

    private fun blockDomain(domain: String) {
        blockedRequests[domain]?.let {
            blockedRequests[domain] = it.copy(userAction = UserAction.BLOCKED)
        }
        Log.i(TAG, "User blocked domain: $domain")
    }

    fun stopVpn() {
        isRunning.set(false)
        vpnThread?.interrupt()
        vpnThread = null
        vpnInterface?.close()
        vpnInterface = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.i(TAG, "VPN stopped")
    }

    fun isVpnRunning(): Boolean = isRunning.get()

    fun getBlockedRequests(): List<BlockedRequest> = blockedRequests.values.toList()

    fun getBlockedCount(): Int = blockedRequests.size

    private fun buildVpnNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, OplVpnService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Online Picket Line Active")
            .setContentText("Monitoring network traffic for labor actions")
            .setContentIntent(pending)
            .addAction(0, "Stop", stopPending)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannels() {
        val vpnChannel = NotificationChannel(
            CHANNEL_ID,
            "VPN Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows when the VPN protection is active"
        }

        val alertChannel = NotificationChannel(
            ALERT_CHANNEL_ID,
            "Labor Action Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when a site or app is connected to an active labor dispute"
        }

        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(vpnChannel)
        nm.createNotificationChannel(alertChannel)
    }

    override fun onDestroy() {
        stopVpn()
        instance = null
        super.onDestroy()
    }

    override fun onRevoke() {
        stopVpn()
        super.onRevoke()
    }
}
