package com.onlinepicketline.opl.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.onlinepicketline.opl.R
import com.onlinepicketline.opl.data.model.Geofence
import com.onlinepicketline.opl.data.repository.OplRepository
import com.onlinepicketline.opl.databinding.ActivityMainBinding
import com.onlinepicketline.opl.util.LocationUtils
import com.onlinepicketline.opl.util.SecureStorage
import com.onlinepicketline.opl.vpn.OplVpnService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: OplRepository
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var geofenceAdapter: GeofenceAdapter

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startVpnService()
        } else {
            Toast.makeText(this, "VPN permission required for traffic monitoring", Toast.LENGTH_LONG).show()
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            loadData()
        } else {
            Toast.makeText(this, "Location permission needed for strike proximity alerts", Toast.LENGTH_LONG).show()
            // Still load blocklist data without location
            loadBlocklistOnly()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        repository = OplRepository(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupViews()
        checkApiKeyAndLoad()
    }

    private fun setupViews() {
        geofenceAdapter = GeofenceAdapter()
        binding.nearbyStrikesRecycler.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = geofenceAdapter
        }

        binding.vpnToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                requestVpnPermission()
            } else {
                OplVpnService.instance?.stopVpn()
                updateVpnStatus(false)
            }
        }

        binding.refreshButton.setOnClickListener { loadData() }

        binding.fabSnapshot.setOnClickListener {
            startActivity(Intent(this, GpsSnapshotActivity::class.java))
        }

        binding.fabSubmit.setOnClickListener {
            startActivity(Intent(this, SubmitStrikeActivity::class.java))
        }
    }

    private fun checkApiKeyAndLoad() {
        if (!SecureStorage.hasApiKey(this)) {
            showApiKeySetup()
            return
        }
        requestLocationAndLoad()
    }

    private fun showApiKeySetup() {
        binding.apiKeySetupGroup.visibility = View.VISIBLE
        binding.mainContentGroup.visibility = View.GONE

        binding.saveApiKeyButton.setOnClickListener {
            val key = binding.apiKeyInput.text.toString().trim()
            if (key.startsWith("opl_") && key.length == 68) {
                SecureStorage.setApiKey(this, key)
                binding.apiKeySetupGroup.visibility = View.GONE
                binding.mainContentGroup.visibility = View.VISIBLE
                requestLocationAndLoad()
            } else {
                binding.apiKeyInput.error = "Invalid API key format (must start with opl_ and be 68 characters)"
            }
        }
    }

    private fun requestLocationAndLoad() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            loadData()
        }
    }

    private fun loadData() {
        binding.progressBar.visibility = View.VISIBLE
        binding.errorText.visibility = View.GONE

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            loadBlocklistOnly()
            return
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    fetchMobileData(location.latitude, location.longitude)
                } else {
                    // Use cached region center if available
                    val cached = repository.getCachedRegionCenter()
                    if (cached != null) {
                        fetchMobileData(cached.lat, cached.lng)
                    } else {
                        loadBlocklistOnly()
                    }
                }
            }
            .addOnFailureListener {
                loadBlocklistOnly()
            }
    }

    private fun fetchMobileData(lat: Double, lng: Double) {
        lifecycleScope.launch {
            val result = repository.getMobileData(lat, lng)
            binding.progressBar.visibility = View.GONE

            result.fold(
                onSuccess = { data ->
                    if (data != null) {
                        updateDashboard(data.blocklist.totalUrls, data.blocklist.totalEmployers)
                        updateGeofenceList(data.geofences.all)
                    } else {
                        // 304 — use cached data
                        val cached = repository.getCachedData()
                        if (cached != null) {
                            updateDashboard(cached.blocklist.totalUrls, cached.blocklist.totalEmployers)
                            updateGeofenceList(cached.geofences.all)
                        }
                    }
                },
                onFailure = { error ->
                    showError(error.message ?: "Failed to load data")
                    // Fall back to cached
                    val cached = repository.getCachedData()
                    if (cached != null) {
                        updateDashboard(cached.blocklist.totalUrls, cached.blocklist.totalEmployers)
                        updateGeofenceList(cached.geofences.all)
                    }
                }
            )
        }
    }

    private fun loadBlocklistOnly() {
        binding.progressBar.visibility = View.GONE
        val cached = repository.getCachedData()
        if (cached != null) {
            updateDashboard(cached.blocklist.totalUrls, cached.blocklist.totalEmployers)
        } else {
            showError("Enable location permission for full functionality")
        }
    }

    private fun updateDashboard(totalUrls: Int, totalEmployers: Int) {
        binding.blockedSitesCount.text = totalUrls.toString()
        binding.employerCount.text = totalEmployers.toString()
        val vpnRunning = OplVpnService.instance?.isVpnRunning() == true
        binding.vpnToggle.isChecked = vpnRunning
        updateVpnStatus(vpnRunning)
        binding.blockedCount.text = (OplVpnService.instance?.getBlockedCount() ?: 0).toString()
    }

    private fun updateGeofenceList(geofences: List<Geofence>) {
        geofenceAdapter.submitList(geofences)
        binding.nearbyStrikesHeader.visibility =
            if (geofences.isNotEmpty()) View.VISIBLE else View.GONE
        binding.noStrikesNearby.visibility =
            if (geofences.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun updateVpnStatus(running: Boolean) {
        binding.vpnStatusText.text = if (running) "Protection Active" else "Protection Off"
        binding.vpnStatusIcon.setImageResource(
            if (running) R.mipmap.ic_launcher_foreground
            else R.mipmap.ic_launcher_background
        )
    }

    private fun showError(message: String) {
        binding.errorText.text = message
        binding.errorText.visibility = View.VISIBLE
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
        val intent = Intent(this, OplVpnService::class.java).apply {
            action = OplVpnService.ACTION_START
        }
        startForegroundService(intent)
        updateVpnStatus(true)
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
            R.id.action_refresh -> {
                loadData()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        // Update VPN status
        val vpnRunning = OplVpnService.instance?.isVpnRunning() == true
        binding.vpnToggle.setOnCheckedChangeListener(null)
        binding.vpnToggle.isChecked = vpnRunning
        binding.vpnToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) requestVpnPermission() else {
                OplVpnService.instance?.stopVpn()
                updateVpnStatus(false)
            }
        }
        updateVpnStatus(vpnRunning)
        binding.blockedCount.text = (OplVpnService.instance?.getBlockedCount() ?: 0).toString()
    }
}
