package com.onlinepicketline.opl.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.onlinepicketline.opl.BuildConfig
import com.onlinepicketline.opl.databinding.ActivityAboutBinding

/**
 * About screen showing OPL's mission, values, and three pillars.
 */
class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "About Online Picket Line"

        binding.versionText.text = "Version ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})"

        setupLinks()
    }

    private fun setupLinks() {
        binding.websiteButton.setOnClickListener {
            openUrl("https://onlinepicketline.com")
        }
        binding.sourceCodeButton.setOnClickListener {
            openUrl("https://github.com/oplfun/opl-for-android")
        }
        binding.reportIssueButton.setOnClickListener {
            openUrl("https://github.com/oplfun/opl-for-android/issues")
        }
        binding.privacyPolicyButton.setOnClickListener {
            openUrl("https://onlinepicketline.com/privacy")
        }
        binding.termsOfServiceButton.setOnClickListener {
            openUrl("https://onlinepicketline.com/terms")
        }
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
