package com.onlinepicketline.opl.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.onlinepicketline.opl.databinding.ActivityBlockPageBinding

/**
 * Full-screen "block page" shown when a user taps a blocked-domain notification.
 * Displays employer info, action details, and lets the user proceed or go back.
 */
class BlockPageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBlockPageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlockPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val domain = intent.getStringExtra("domain") ?: ""
        val employer = intent.getStringExtra("employer") ?: "Unknown"
        val actionType = intent.getStringExtra("actionType") ?: "strike"
        val actionId = intent.getStringExtra("actionId") ?: ""

        binding.employerName.text = employer
        binding.actionTypeText.text = "Active ${actionType.replaceFirstChar { it.uppercase() }}"
        binding.domainText.text = "Domain: $domain"
        binding.messageText.text =
            "Workers at $employer have an active $actionType. " +
                    "By continuing to this site, you may be crossing a digital picket line."

        binding.respectButton.setOnClickListener {
            Toast.makeText(this, "Thank you for your solidarity!", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.proceedButton.setOnClickListener {
            // Allow the domain in the VPN service
            com.onlinepicketline.opl.vpn.OplVpnService.instance?.let { vpn ->
                val intent = android.content.Intent(this, com.onlinepicketline.opl.vpn.OplVpnService::class.java).apply {
                    action = com.onlinepicketline.opl.vpn.OplVpnService.ACTION_ALLOW_DOMAIN
                    putExtra(com.onlinepicketline.opl.vpn.OplVpnService.EXTRA_DOMAIN, domain)
                }
                startService(intent)
            }
            finish()
        }
    }
}
