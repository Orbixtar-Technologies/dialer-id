package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.example.ui.dialer.DialerViewModel
import com.example.ui.main.MainAppShell
import com.example.ui.theme.DialerIDTheme

class MainActivity : ComponentActivity() {

    private val dialerViewModel: DialerViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Permissions granted/denied handled gracefully
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check & request runtime permissions for audio & notifications
        requestRequiredPermissions()

        val profile = com.example.data.repository.DialerRepository.getInstance(this).userProfile.value
        if (!profile.isGuest) {
            com.example.service.SipRegisterService.start(this)
        }

        handleIntent(intent)

        setContent {
            DialerIDTheme {
                MainAppShell(
                    dialerViewModel = dialerViewModel
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val profile = com.example.data.repository.DialerRepository.getInstance(this).userProfile.value
        if (!profile.isGuest) {
            if (profile.sipConfig?.hasUsableCredentials() == true) {
                com.example.service.SipRegisterService.refresh(this)
            } else {
                com.example.service.SipRegisterService.start(this)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_DIAL || intent?.action == Intent.ACTION_VIEW) {
            val data = intent.data
            if (data != null && data.scheme == "tel") {
                val phoneNumber = data.schemeSpecificPart
                val sanitized = com.example.util.PhoneNumberSanitizer.sanitizeDestination(phoneNumber.orEmpty())
                if (!sanitized.isNullOrBlank()) {
                    dialerViewModel.onNumberChanged(sanitized)
                }
            }
        }
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }
}
