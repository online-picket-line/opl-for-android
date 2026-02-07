package com.onlinepicketline.opl.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import com.onlinepicketline.opl.util.SecureStorage
import com.onlinepicketline.opl.vpn.OplVpnService

/**
 * Restarts the VPN service after device boot if auto-block was enabled.
 */
class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        
        val prefs = context.getSharedPreferences("opl_settings", Context.MODE_PRIVATE)
        val autoBlockEnabled = prefs.getBoolean("auto_block_enabled", false)
        
        if (!autoBlockEnabled) return
        
        // Only restart if we have an API key and VPN permission
        val secureStorage = SecureStorage(context)
        if (secureStorage.getApiKey() == null) return
        
        // Check if VPN permission was previously granted
        val vpnIntent = VpnService.prepare(context)
        if (vpnIntent == null) {
            // Permission already granted, start the service
            val serviceIntent = Intent(context, OplVpnService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}
