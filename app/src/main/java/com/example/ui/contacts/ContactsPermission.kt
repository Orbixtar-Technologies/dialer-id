package com.example.ui.contacts

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

enum class ContactsPermissionStatus {
    /** `READ_CONTACTS` is held; queries will return real address book rows. */
    GRANTED,

    /** Not granted yet, but the system dialog can still be shown. */
    DENIED,

    /** Denied for good; only the app settings screen can restore access. */
    PERMANENTLY_DENIED
}

/**
 * Runtime permission handle for the device address book.
 *
 * [hasAsked] lets callers request once on first use without ever re-prompting
 * a user who has already refused.
 */
@Stable
class ContactsPermissionState internal constructor(
    val status: ContactsPermissionStatus,
    val hasAsked: Boolean,
    private val onRequest: () -> Unit,
    private val onOpenSettings: () -> Unit
) {
    val isGranted: Boolean get() = status == ContactsPermissionStatus.GRANTED

    fun request() = onRequest()

    fun openAppSettings() = onOpenSettings()
}

/**
 * Tracks `READ_CONTACTS` across the permission dialog and any grant the user
 * makes from system settings while the app is backgrounded.
 */
@Composable
fun rememberContactsPermissionState(
    onGrantedChanged: (Boolean) -> Unit = {}
): ContactsPermissionState {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val grantedCallback by rememberUpdatedState(onGrantedChanged)

    var hasAsked by rememberSaveable { mutableStateOf(false) }
    var status by remember { mutableStateOf(readStatus(context, activity, hasAsked)) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAsked = true
        status = when {
            granted -> ContactsPermissionStatus.GRANTED
            activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.READ_CONTACTS
            ) -> ContactsPermissionStatus.PERMANENTLY_DENIED

            else -> ContactsPermissionStatus.DENIED
        }
        grantedCallback(granted)
    }

    DisposableEffect(lifecycleOwner, activity, hasAsked) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val refreshed = readStatus(context, activity, hasAsked)
                if (refreshed != status) {
                    status = refreshed
                    grantedCallback(refreshed == ContactsPermissionStatus.GRANTED)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return ContactsPermissionState(
        status = status,
        hasAsked = hasAsked,
        onRequest = { launcher.launch(Manifest.permission.READ_CONTACTS) },
        onOpenSettings = {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null)
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(intent)
        }
    )
}

private fun readStatus(
    context: Context,
    activity: Activity?,
    hasAsked: Boolean
): ContactsPermissionStatus {
    val granted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_CONTACTS
    ) == PackageManager.PERMISSION_GRANTED

    return when {
        granted -> ContactsPermissionStatus.GRANTED
        // Before the first prompt the rationale flag is also false, so a
        // permanent denial can only be inferred once the user has been asked.
        hasAsked && activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.READ_CONTACTS
        ) -> ContactsPermissionStatus.PERMANENTLY_DENIED

        else -> ContactsPermissionStatus.DENIED
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
