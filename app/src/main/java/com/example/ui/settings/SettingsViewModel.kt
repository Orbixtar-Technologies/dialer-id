package com.example.ui.settings

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.SipConfig
import com.example.data.repository.DialerRepository
import com.example.service.CallManager
import com.example.service.SipRegisterService
import com.example.service.sip.SipEngine
import com.example.service.sip.SipTestResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val autoSpeakerphone: Boolean = false,
    val hdAudioQuality: Boolean = true,
    val noiseReduction: Boolean = true,
    val telecomSelfManaged: Boolean = true,
    val isEditProfileDialogVisible: Boolean = false,
    val editDisplayName: String = "",
    val editPhoneNumber: String = "",
    val editOrganization: String = "",
    val editAccountRole: String = "",
    val isEditSipDialogVisible: Boolean = false,
    val editSipHost: String = "sip.sipup.org",
    val editSipPort: String = "5060",
    val editSipUsername: String = "",
    val editSipPassword: String = "",
    val editSipDeviceId: String = "",
    val editSipCallerId: String = "",
    val isTestingSip: Boolean = false,
    val sipTestResult: SipTestResult? = null,
    val toastMessage: String? = null
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DialerRepository.getInstance(application)
    private val sipEngine = CallManager.getInstance(application).sipEngine
    val userProfile = repository.userProfile
    val registrationState = sipEngine.registrationState
    val sdpDiagnosticDump = SipEngine.lastSdpDiagnosticDump

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun toggleAutoSpeakerphone(value: Boolean) {
        _uiState.value = _uiState.value.copy(autoSpeakerphone = value)
    }

    fun toggleHdAudio(value: Boolean) {
        _uiState.value = _uiState.value.copy(hdAudioQuality = value)
    }

    fun toggleNoiseReduction(value: Boolean) {
        _uiState.value = _uiState.value.copy(noiseReduction = value)
    }

    fun setPreferredCodec(codec: String) {
        repository.updatePreferredCodec(codec)
        _uiState.value = _uiState.value.copy(
            toastMessage = when (codec) {
                "G711A" -> "Telephony Codec set to G.711a (PCMA - Europe & Int'l)"
                "G711U" -> "Telephony Codec set to G.711u (PCMU - North America & Japan)"
                else -> "Telephony Codec set to Auto Dual-Stack (PCMA / PCMU)"
            }
        )
    }

    fun showEditProfileDialog() {
        val current = userProfile.value
        _uiState.value = _uiState.value.copy(
            isEditProfileDialogVisible = true,
            editDisplayName = current.displayName,
            editPhoneNumber = current.phoneNumber,
            editOrganization = current.organization,
            editAccountRole = current.accountRole
        )
    }

    fun hideEditProfileDialog() {
        _uiState.value = _uiState.value.copy(isEditProfileDialogVisible = false)
    }

    fun onEditDisplayNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(editDisplayName = name)
    }

    fun onEditPhoneNumberChanged(phone: String) {
        _uiState.value = _uiState.value.copy(editPhoneNumber = phone)
    }

    fun onEditOrganizationChanged(org: String) {
        _uiState.value = _uiState.value.copy(editOrganization = org)
    }

    fun onEditAccountRoleChanged(role: String) {
        _uiState.value = _uiState.value.copy(editAccountRole = role)
    }

    fun saveProfile() {
        val state = _uiState.value
        repository.updateProfile(
            displayName = state.editDisplayName.ifBlank { "Operator" },
            phoneNumber = state.editPhoneNumber,
            organization = state.editOrganization.ifBlank { "Secure Telecom Network" },
            accountRole = state.editAccountRole.ifBlank { "Verified Operator" }
        )
        _uiState.value = _uiState.value.copy(
            isEditProfileDialogVisible = false,
            toastMessage = "Profile updated & synced to Realtime DB"
        )
    }

    fun showEditSipDialog() {
        val currentProfile = userProfile.value
        val currentSip = currentProfile.sipConfig
        _uiState.value = _uiState.value.copy(
            isEditSipDialogVisible = true,
            editSipHost = currentSip?.host?.ifBlank { "sip.sipup.org" } ?: "sip.sipup.org",
            editSipPort = "${currentSip?.port ?: 5060}",
            editSipUsername = currentSip?.username.orEmpty(),
            editSipPassword = currentSip?.password.orEmpty(),
            editSipDeviceId = currentSip?.deviceId.orEmpty(),
            editSipCallerId = currentSip?.callerId?.ifBlank { currentProfile.selectedCallerId } ?: currentProfile.selectedCallerId,
            sipTestResult = null,
            isTestingSip = false
        )
    }

    fun hideEditSipDialog() {
        _uiState.value = _uiState.value.copy(isEditSipDialogVisible = false, sipTestResult = null)
    }

    fun onEditSipHostChanged(host: String) {
        _uiState.value = _uiState.value.copy(editSipHost = host, sipTestResult = null)
    }

    fun onEditSipPortChanged(port: String) {
        _uiState.value = _uiState.value.copy(editSipPort = port, sipTestResult = null)
    }

    fun onEditSipUsernameChanged(user: String) {
        _uiState.value = _uiState.value.copy(editSipUsername = user, sipTestResult = null)
    }

    fun onEditSipPasswordChanged(pass: String) {
        _uiState.value = _uiState.value.copy(editSipPassword = pass, sipTestResult = null)
    }

    fun onEditSipDeviceIdChanged(devId: String) {
        _uiState.value = _uiState.value.copy(editSipDeviceId = devId, sipTestResult = null)
    }

    fun onEditSipCallerIdChanged(cid: String) {
        _uiState.value = _uiState.value.copy(editSipCallerId = cid, sipTestResult = null)
    }

    private fun configFromEditor(): SipConfig {
        val state = _uiState.value
        return SipConfig(
            host = state.editSipHost.trim(),
            port = state.editSipPort.trim().toIntOrNull() ?: 5060,
            username = state.editSipUsername.trim(),
            password = state.editSipPassword,
            callerId = state.editSipCallerId.trim(),
            deviceId = state.editSipDeviceId.trim()
        )
    }

    fun testSipConnection() {
        val config = configFromEditor()
        if (!config.hasUsableCredentials()) {
            _uiState.value = _uiState.value.copy(
                isTestingSip = false,
                sipTestResult = SipTestResult(false, 0, "Host, username, and password are required", 0)
            )
            return
        }

        _uiState.value = _uiState.value.copy(isTestingSip = true, sipTestResult = null)

        viewModelScope.launch {
            val result = sipEngine.testSipConnection(config)
            _uiState.value = _uiState.value.copy(
                isTestingSip = false,
                sipTestResult = result
            )
        }
    }

    fun saveSipConfig() {
        val config = configFromEditor()
        if (!config.hasUsableCredentials()) {
            _uiState.value = _uiState.value.copy(
                toastMessage = "Host, username, and password are required"
            )
            return
        }
        if (userProfile.value.isGuest) {
            _uiState.value = _uiState.value.copy(
                toastMessage = "Sign in to save SIP credentials. Guest mode cannot use a shared trunk."
            )
            return
        }

        repository.updateSipConfig(config)
        sipEngine.register(config, force = true)
        SipRegisterService.start(getApplication())

        _uiState.value = _uiState.value.copy(
            isEditSipDialogVisible = false,
            toastMessage = "SIP configuration saved. Password stays on this device only."
        )
    }

    fun refreshRegistration() {
        val config = userProfile.value.sipConfig
        if (config == null || !config.hasUsableCredentials()) {
            _uiState.value = _uiState.value.copy(toastMessage = "Configure SIP credentials first")
            return
        }
        sipEngine.refreshNow()
        _uiState.value = _uiState.value.copy(toastMessage = "Refreshing SIP registration...")
    }

    fun forceReRegister() {
        val config = userProfile.value.sipConfig
        if (config == null || !config.hasUsableCredentials() || userProfile.value.isGuest) {
            _uiState.value = _uiState.value.copy(toastMessage = "Configure SIP credentials first")
            return
        }
        sipEngine.register(config, force = true)
        _uiState.value = _uiState.value.copy(toastMessage = "Re-registering SIP account...")
    }

    fun copyUidToClipboard(uid: String) {
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("User UID", uid)
        clipboard?.setPrimaryClip(clip)
        _uiState.value = _uiState.value.copy(toastMessage = "User UID copied to clipboard")
    }

    fun copySdpDumpToClipboard() {
        val dump = sdpDiagnosticDump.value ?: return
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("SIP SDP Diagnostic Dump", dump.formattedReport)
        clipboard?.setPrimaryClip(clip)
        _uiState.value = _uiState.value.copy(toastMessage = "SIP SDP Diagnostic dump copied to clipboard")
    }

    fun clearToastMessage() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        // Shared SipEngine is owned by CallManager; do not stop the Core here.
    }
}
