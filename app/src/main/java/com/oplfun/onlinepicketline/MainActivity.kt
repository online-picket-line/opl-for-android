package com.onlinepicketline.onlinepicketline

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.onlinepicketline.onlinepicketline.data.repository.DisputeRepository
import com.onlinepicketline.onlinepicketline.ui.DisputeAdapter
import com.onlinepicketline.onlinepicketline.vpn.PicketLineVpnService
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var repository: DisputeRepository
    private lateinit var statusText: TextView
    private lateinit var toggleButton: Button
    private lateinit var refreshButton: Button
    private lateinit var disputesList: RecyclerView
    private lateinit var disputeAdapter: DisputeAdapter
    
    private var isVpnRunning = false
    
    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startVpnService()
        } else {
            Toast.makeText(this, "VPN permission denied", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        repository = DisputeRepository.getInstance(this)
        
        setupViews()
        setupRecyclerView()
        handleIntent(intent)
        refreshDisputes()
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIntent(it) }
    }
    
    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            PicketLineVpnService.ACTION_BLOCK_NOTIFICATION -> {
                val domain = intent.getStringExtra(PicketLineVpnService.EXTRA_DOMAIN)
                domain?.let { showBlockDialog(it) }
            }
        }
    }
    
    private fun setupViews() {
        statusText = findViewById(R.id.status_text)
        toggleButton = findViewById(R.id.toggle_button)
        refreshButton = findViewById(R.id.refresh_button)
        
        toggleButton.setOnClickListener {
            if (isVpnRunning) {
                stopVpnService()
            } else {
                requestVpnPermission()
            }
        }
        
        refreshButton.setOnClickListener {
            refreshDisputes()
        }
        
        updateUI()
    }
    
    private fun setupRecyclerView() {
        disputesList = findViewById(R.id.disputes_list)
        disputeAdapter = DisputeAdapter()
        disputesList.apply {
            adapter = disputeAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
    }
    
    private fun requestVpnPermission() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            startVpnService()
        }
    }
    
    private fun startVpnService() {
        val intent = Intent(this, PicketLineVpnService::class.java).apply {
            action = PicketLineVpnService.ACTION_START
        }
        startService(intent)
        isVpnRunning = true
        updateUI()
        Toast.makeText(this, "VPN started", Toast.LENGTH_SHORT).show()
    }
    
    private fun stopVpnService() {
        val intent = Intent(this, PicketLineVpnService::class.java).apply {
            action = PicketLineVpnService.ACTION_STOP
        }
        startService(intent)
        isVpnRunning = false
        updateUI()
        Toast.makeText(this, "VPN stopped", Toast.LENGTH_SHORT).show()
    }
    
    private fun refreshDisputes() {
        lifecycleScope.launch {
            val result = repository.fetchDisputes(forceRefresh = true)
            result.fold(
                onSuccess = { disputes ->
                    disputeAdapter.submitList(disputes)
                    Toast.makeText(
                        this@MainActivity,
                        "Loaded ${disputes.size} active disputes",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onFailure = { error ->
                    Toast.makeText(
                        this@MainActivity,
                        "Failed to fetch disputes: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }
    
    private fun showBlockDialog(domain: String) {
        val dispute = repository.findDisputeForDomain(domain) ?: return
        
        AlertDialog.Builder(this)
            .setTitle("Labor Dispute Detected")
            .setMessage(buildString {
                append("Company: ${dispute.companyName}\n")
                append("Domain: $domain\n\n")
                append("Dispute Type: ${dispute.disputeType}\n")
                append("Description: ${dispute.description}\n")
                dispute.union?.let { append("Union: $it\n") }
                append("\nAccess to this site has been blocked.")
            })
            .setPositiveButton("Keep Blocking") { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton("Allow This Time") { dialog, _ ->
                // Allow this one time - user would need to manually visit
                Toast.makeText(this, "Allowed for this session", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNeutralButton("Always Allow") { dialog, _ ->
                repository.allowDomain(domain)
                Toast.makeText(this, "$domain added to allowed list", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun updateUI() {
        if (isVpnRunning) {
            statusText.text = "Status: Active - Monitoring traffic"
            toggleButton.text = "Stop Protection"
        } else {
            statusText.text = "Status: Inactive"
            toggleButton.text = "Start Protection"
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_allowed_domains -> {
                showAllowedDomains()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showAllowedDomains() {
        val allowedDomains = repository.getAllowedDomains()
        
        if (allowedDomains.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Allowed Domains")
                .setMessage("No domains have been marked as allowed.")
                .setPositiveButton("OK", null)
                .show()
        } else {
            val items = allowedDomains.toTypedArray()
            AlertDialog.Builder(this)
                .setTitle("Allowed Domains")
                .setItems(items) { _, which ->
                    val domain = items[which]
                    showRemoveAllowedDomainDialog(domain)
                }
                .setNegativeButton("Close", null)
                .show()
        }
    }
    
    private fun showRemoveAllowedDomainDialog(domain: String) {
        AlertDialog.Builder(this)
            .setTitle("Remove Allowed Domain")
            .setMessage("Remove $domain from the allowed list?")
            .setPositiveButton("Remove") { _, _ ->
                repository.removeAllowedDomain(domain)
                Toast.makeText(this, "Removed $domain", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
