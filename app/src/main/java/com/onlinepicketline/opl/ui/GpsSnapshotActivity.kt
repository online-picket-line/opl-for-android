package com.onlinepicketline.opl.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.onlinepicketline.opl.data.model.ActiveStrike
import com.onlinepicketline.opl.data.model.GpsSnapshotRequest
import com.onlinepicketline.opl.data.repository.OplRepository
import com.onlinepicketline.opl.databinding.ActivityGpsSnapshotBinding
import kotlinx.coroutines.launch

/**
 * GPS Snapshot screen: lets users submit their current GPS location
 * (or a manually entered location) tied to an active strike.
 */
class GpsSnapshotActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGpsSnapshotBinding
    private lateinit var repository: OplRepository
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var activeStrikes: List<ActiveStrike> = emptyList()
    private var selectedStrike: ActiveStrike? = null
    private var currentLat: Double? = null
    private var currentLng: Double? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            captureCurrentLocation()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGpsSnapshotBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "GPS Snapshot"

        repository = OplRepository(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupViews()
        loadActiveStrikes()
    }

    private fun setupViews() {
        binding.useCurrentLocationButton.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                locationPermissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                )
            } else {
                captureCurrentLocation()
            }
        }

        binding.lookupAddressButton.setOnClickListener {
            val address = binding.manualAddressInput.text.toString().trim()
            if (address.isNotEmpty()) {
                geocodeAddress(address)
            } else {
                binding.manualAddressInput.error = "Enter an address"
            }
        }

        binding.lookupCoordsButton.setOnClickListener {
            val lat = binding.latitudeInput.text.toString().toDoubleOrNull()
            val lng = binding.longitudeInput.text.toString().toDoubleOrNull()
            if (lat != null && lng != null) {
                currentLat = lat
                currentLng = lng
                reverseGeocode(lat, lng)
            } else {
                Toast.makeText(this, "Enter valid latitude and longitude", Toast.LENGTH_SHORT).show()
            }
        }

        binding.submitButton.setOnClickListener { submitSnapshot() }
    }

    private fun loadActiveStrikes() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            repository.getActiveStrikes().fold(
                onSuccess = { strikes ->
                    activeStrikes = strikes
                    binding.progressBar.visibility = View.GONE
                    if (strikes.isEmpty()) {
                        Toast.makeText(this@GpsSnapshotActivity, "No active strikes found", Toast.LENGTH_LONG).show()
                        return@fold
                    }
                    val adapter = ArrayAdapter(
                        this@GpsSnapshotActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        strikes.map { "${it.employerName} — ${it.organization ?: it.actionType}" }
                    )
                    binding.strikeSpinner.adapter = adapter
                },
                onFailure = { error ->
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@GpsSnapshotActivity, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    private fun captureCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return

        binding.progressBar.visibility = View.VISIBLE
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                binding.progressBar.visibility = View.GONE
                if (location != null) {
                    currentLat = location.latitude
                    currentLng = location.longitude
                    binding.latitudeInput.setText(location.latitude.toString())
                    binding.longitudeInput.setText(location.longitude.toString())
                    reverseGeocode(location.latitude, location.longitude)
                    Toast.makeText(this, "Location captured!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Could not get location. Try again.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Location error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun geocodeAddress(address: String) {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            repository.geocode(address).fold(
                onSuccess = { result ->
                    binding.progressBar.visibility = View.GONE
                    currentLat = result.latitude
                    currentLng = result.longitude
                    binding.latitudeInput.setText(result.latitude.toString())
                    binding.longitudeInput.setText(result.longitude.toString())
                    binding.resolvedAddressText.text = result.displayName ?: address
                    binding.resolvedAddressText.visibility = View.VISIBLE
                },
                onFailure = { error ->
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@GpsSnapshotActivity, "Geocode failed: ${error.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    private fun reverseGeocode(lat: Double, lng: Double) {
        lifecycleScope.launch {
            repository.reverseGeocode(lat, lng).fold(
                onSuccess = { result ->
                    val displayAddress = result.displayName ?: listOfNotNull(
                        result.address, result.city, result.state
                    ).joinToString(", ")
                    binding.resolvedAddressText.text = displayAddress
                    binding.resolvedAddressText.visibility = View.VISIBLE
                    binding.manualAddressInput.setText(displayAddress)
                },
                onFailure = { /* Non-critical — just don't show resolved address */ }
            )
        }
    }

    private fun submitSnapshot() {
        val lat = currentLat
        val lng = currentLng
        val pos = binding.strikeSpinner.selectedItemPosition

        if (lat == null || lng == null) {
            Toast.makeText(this, "Capture or enter a location first", Toast.LENGTH_SHORT).show()
            return
        }
        if (pos < 0 || pos >= activeStrikes.size) {
            Toast.makeText(this, "Select a strike", Toast.LENGTH_SHORT).show()
            return
        }

        selectedStrike = activeStrikes[pos]
        val notes = binding.notesInput.text.toString().trim()
        val address = binding.manualAddressInput.text.toString().trim()

        binding.submitButton.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            repository.submitGpsSnapshot(
                GpsSnapshotRequest(
                    actionId = selectedStrike!!.id,
                    latitude = lat,
                    longitude = lng,
                    address = address.ifBlank { null },
                    notes = notes.ifBlank { null }
                )
            ).fold(
                onSuccess = {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@GpsSnapshotActivity, "Snapshot submitted! Thank you for your solidarity.", Toast.LENGTH_LONG).show()
                    finish()
                },
                onFailure = { error ->
                    binding.progressBar.visibility = View.GONE
                    binding.submitButton.isEnabled = true
                    Toast.makeText(this@GpsSnapshotActivity, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
