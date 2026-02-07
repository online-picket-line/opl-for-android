package com.onlinepicketline.opl.ui

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.onlinepicketline.opl.data.model.*
import com.onlinepicketline.opl.data.repository.OplRepository
import com.onlinepicketline.opl.databinding.ActivitySubmitStrikeBinding
import kotlinx.coroutines.launch

/**
 * Strike submission wizard — allows users to submit a new labor action
 * from the mobile app, mirroring the web submission form.
 */
class SubmitStrikeActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySubmitStrikeBinding
    private lateinit var repository: OplRepository
    private var geocodedCoords: GpsCoordinates? = null

    private val actionTypes = listOf("strike", "walkout", "slowdown", "picket", "boycott", "other")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubmitStrikeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Submit Labor Action"

        repository = OplRepository(this)
        setupViews()
    }

    private fun setupViews() {
        binding.actionTypeSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            actionTypes.map { it.replaceFirstChar { c -> c.uppercase() } }
        )

        binding.geocodeButton.setOnClickListener {
            val address = binding.locationInput.text.toString().trim()
            if (address.isNotEmpty()) {
                geocodeLocation(address)
            } else {
                binding.locationInput.error = "Enter a location"
            }
        }

        binding.submitButton.setOnClickListener { submitStrike() }
    }

    private fun geocodeLocation(address: String) {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            repository.geocode(address).fold(
                onSuccess = { result ->
                    binding.progressBar.visibility = View.GONE
                    geocodedCoords = GpsCoordinates(result.latitude, result.longitude)
                    binding.geocodeResult.text = result.displayName ?: "Coordinates: ${result.latitude}, ${result.longitude}"
                    binding.geocodeResult.visibility = View.VISIBLE
                },
                onFailure = { error ->
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@SubmitStrikeActivity, "Geocode failed: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun submitStrike() {
        val employerName = binding.employerNameInput.text.toString().trim()
        val organization = binding.organizationInput.text.toString().trim()
        val location = binding.locationInput.text.toString().trim()
        val startDate = binding.startDateInput.text.toString().trim()
        val description = binding.descriptionInput.text.toString().trim()
        val durationStr = binding.durationInput.text.toString().trim()
        val demands = binding.demandsInput.text.toString().trim()
        val contactInfo = binding.contactInfoInput.text.toString().trim()
        val learnMoreUrl = binding.learnMoreUrlInput.text.toString().trim()
        val website = binding.employerWebsiteInput.text.toString().trim()
        val industry = binding.employerIndustryInput.text.toString().trim()

        // Validate required fields
        if (employerName.isBlank()) { binding.employerNameInput.error = "Required"; return }
        if (organization.isBlank()) { binding.organizationInput.error = "Required"; return }
        if (location.isBlank()) { binding.locationInput.error = "Required"; return }
        if (startDate.isBlank()) { binding.startDateInput.error = "Required (YYYY-MM-DD)"; return }
        if (description.isBlank()) { binding.descriptionInput.error = "Required"; return }

        // Validate date format
        val dateRegex = Regex("""\d{4}-\d{2}-\d{2}""")
        if (!dateRegex.matches(startDate)) {
            binding.startDateInput.error = "Use YYYY-MM-DD format"
            return
        }

        val actionType = actionTypes[binding.actionTypeSpinner.selectedItemPosition]
        val duration = durationStr.toIntOrNull() ?: 30

        binding.submitButton.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            repository.submitStrike(
                StrikeSubmissionRequest(
                    employer = EmployerSubmission(
                        name = employerName,
                        industry = industry.ifBlank { null },
                        website = website.ifBlank { null }
                    ),
                    action = ActionSubmission(
                        organization = organization,
                        actionType = actionType,
                        location = location,
                        startDate = startDate,
                        durationDays = duration,
                        description = description,
                        demands = demands.ifBlank { null },
                        contactInfo = contactInfo.ifBlank { null },
                        learnMoreUrl = learnMoreUrl.ifBlank { null },
                        coordinates = geocodedCoords
                    )
                )
            ).fold(
                onSuccess = {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@SubmitStrikeActivity,
                        "Strike submitted for review! Thank you.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                },
                onFailure = { error ->
                    binding.progressBar.visibility = View.GONE
                    binding.submitButton.isEnabled = true
                    Toast.makeText(this@SubmitStrikeActivity, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
