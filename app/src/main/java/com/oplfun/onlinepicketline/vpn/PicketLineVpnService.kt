package com.oplfun.onlinepicketline.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import com.oplfun.onlinepicketline.MainActivity
import com.oplfun.onlinepicketline.R
import com.oplfun.onlinepicketline.data.model.LaborDispute
import com.oplfun.onlinepicketline.data.repository.DisputeRepository
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetAddress
import java.nio.ByteBuffer

/**
 * VPN Service that intercepts network traffic and blocks requests to companies
 * under labor disputes
 */
class PicketLineVpnService : VpnService() {
    
    private var vpnInterface: ParcelFileDescriptor? = null
    private var serviceJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private lateinit var repository: DisputeRepository
    private val blockedDomains = mutableSetOf<String>()
    
    companion object {
        private const val TAG = "PicketLineVpnService"
        private const val NOTIFICATION_CHANNEL_ID = "picketline_vpn"
        private const val NOTIFICATION_ID = 1
        
        const val ACTION_START = "com.oplfun.onlinepicketline.START_VPN"
        const val ACTION_STOP = "com.oplfun.onlinepicketline.STOP_VPN"
        const val ACTION_BLOCK_NOTIFICATION = "com.oplfun.onlinepicketline.BLOCK_NOTIFICATION"
        
        const val EXTRA_DOMAIN = "domain"
        const val EXTRA_DISPUTE = "dispute"
    }
    
    override fun onCreate() {
        super.onCreate()
        repository = DisputeRepository.getInstance(applicationContext)
        createNotificationChannel()
        
        // Fetch initial dispute data
        serviceScope.launch {
            repository.fetchDisputes()
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startVpn()
            }
            ACTION_STOP -> {
                stopVpn()
            }
        }
        return START_STICKY
    }
    
    private fun startVpn() {
        if (vpnInterface != null) {
            Log.d(TAG, "VPN already running")
            return
        }
        
        try {
            // Configure the VPN
            val builder = Builder()
                .setSession("Online Picket Line")
                .addAddress("10.0.0.2", 24)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("8.8.8.8")
                .addDnsServer("8.8.4.4")
            
            vpnInterface = builder.establish()
            
            if (vpnInterface == null) {
                Log.e(TAG, "Failed to establish VPN")
                stopSelf()
                return
            }
            
            // Start foreground service
            startForeground(NOTIFICATION_ID, createNotification())
            
            // Start packet processing
            serviceJob = serviceScope.launch {
                processPackets()
            }
            
            Log.d(TAG, "VPN started successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting VPN", e)
            stopVpn()
        }
    }
    
    private fun stopVpn() {
        Log.d(TAG, "Stopping VPN")
        
        serviceJob?.cancel()
        serviceJob = null
        
        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing VPN interface", e)
        }
        vpnInterface = null
        
        stopForeground(true)
        stopSelf()
    }
    
    private suspend fun processPackets() {
        val vpnInput = FileInputStream(vpnInterface!!.fileDescriptor)
        val vpnOutput = FileOutputStream(vpnInterface!!.fileDescriptor)
        val buffer = ByteBuffer.allocate(32767)
        
        try {
            while (isActive && vpnInterface != null) {
                buffer.clear()
                val length = vpnInput.read(buffer.array())
                
                if (length > 0) {
                    buffer.limit(length)
                    
                    // Parse packet and check if it should be blocked
                    val shouldBlock = checkAndBlockPacket(buffer)
                    
                    if (!shouldBlock) {
                        // Forward the packet
                        buffer.position(0)
                        vpnOutput.write(buffer.array(), 0, length)
                    }
                }
            }
        } catch (e: Exception) {
            if (isActive) {
                Log.e(TAG, "Error processing packets", e)
            }
        }
    }
    
    private fun checkAndBlockPacket(packet: ByteBuffer): Boolean {
        try {
            // Basic IP packet parsing to extract destination
            val version = (packet.get(0).toInt() shr 4) and 0xF
            
            if (version == 4) {
                // IPv4 packet
                val destAddress = ByteArray(4)
                packet.position(16) // Destination IP offset in IPv4
                packet.get(destAddress)
                
                val destIp = InetAddress.getByAddress(destAddress)
                val hostname = destIp.hostName
                
                // Check if this domain is under dispute
                val dispute = repository.findDisputeForDomain(hostname)
                if (dispute != null) {
                    Log.d(TAG, "Blocking traffic to ${dispute.companyName} ($hostname)")
                    notifyBlock(hostname, dispute)
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing packet", e)
        }
        
        return false
    }
    
    private fun notifyBlock(domain: String, dispute: LaborDispute) {
        if (blockedDomains.contains(domain)) {
            return // Already notified about this domain
        }
        blockedDomains.add(domain)
        
        // Create notification about the block
        val intent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_BLOCK_NOTIFICATION
            putExtra(EXTRA_DOMAIN, domain)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 
            domain.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
            .setContentTitle("Labor Dispute Detected")
            .setContentText("${dispute.companyName} is under ${dispute.disputeType}. Tap to learn more.")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(domain.hashCode(), notification)
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 
            0, 
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
            .setContentTitle("Online Picket Line Active")
            .setContentText("Monitoring network traffic for labor disputes")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .build()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "VPN Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when VPN is active"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopVpn()
        serviceScope.cancel()
    }
    
    override fun onRevoke() {
        super.onRevoke()
        stopVpn()
    }
}
