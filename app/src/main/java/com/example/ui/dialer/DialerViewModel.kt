package com.example.ui.dialer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CallerIdItem
import com.example.data.model.UserProfile
import com.example.data.repository.DialerRepository
import com.example.service.ActiveCallInfo
import com.example.service.CallManager
import com.example.service.SipRegisterService
import com.example.ui.common.CountryInfo
import com.example.ui.common.CountryUtils
import com.example.util.PhoneNumberSanitizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class DialerUiState(
    val inputNumber: String = "",
    val formattedDisplay: String = "",
    val detectedCountry: CountryInfo = CountryUtils.COUNTRIES.first(),
    val estimatedRate: Double = 0.015,
    val showZeroBalanceWarning: Boolean = false,
    val showCallerIdDropdown: Boolean = false
)

class DialerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DialerRepository.getInstance(application)
    private val callManager = CallManager.getInstance(application)

    val userProfile: StateFlow<UserProfile> = repository.userProfile
    val allCallerIds: StateFlow<List<CallerIdItem>> = repository.allCallerIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeCallState: StateFlow<ActiveCallInfo> = callManager.callState
    val registrationState = callManager.sipEngine.registrationState

    private val _uiState = MutableStateFlow(DialerUiState())
    val uiState: StateFlow<DialerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                val profile = userProfile.value
                val state = registrationState.value
                if (!profile.isGuest && profile.sipConfig?.hasUsableCredentials() == true && !state.isRegistered) {
                    SipRegisterService.refresh(getApplication())
                }
                kotlinx.coroutines.delay(30_000L)
            }
        }
    }

    fun onScreenOpened() {
        val profile = userProfile.value
        val state = registrationState.value
        if (!profile.isGuest && profile.sipConfig?.hasUsableCredentials() == true && !state.isRegistered) {
            SipRegisterService.refresh(getApplication())
        }
    }

    fun appendDigit(digit: Char) {
        val current = _uiState.value.inputNumber
        if (current.length >= 20) return
        val newNumber = current + digit
        updateNumber(newNumber)
    }

    fun onBackspace() {
        val current = _uiState.value.inputNumber
        if (current.isNotEmpty()) {
            val newNumber = current.dropLast(1)
            updateNumber(newNumber)
        }
    }

    fun onClearAll() {
        updateNumber("")
    }

    fun onNumberChanged(number: String) {
        updateNumber(number)
    }

    fun fillTestNumber(number: String) {
        updateNumber(number)
    }

    private fun updateNumber(number: String) {
        val sanitized = PhoneNumberSanitizer.filterDialInput(number)
        val (country, rate) = CountryUtils.estimateRateForNumber(sanitized)
        val formatted = CountryUtils.formatPhoneNumber(sanitized)
        _uiState.value = _uiState.value.copy(
            inputNumber = sanitized,
            formattedDisplay = formatted,
            detectedCountry = country,
            estimatedRate = rate,
            showZeroBalanceWarning = false
        )
    }

    fun selectCallerId(callerId: CallerIdItem) {
        repository.updateSelectedCallerId(callerId.phoneNumber)
        _uiState.value = _uiState.value.copy(showCallerIdDropdown = false)
    }

    fun toggleCallerIdDropdown(show: Boolean) {
        _uiState.value = _uiState.value.copy(showCallerIdDropdown = show)
    }

    fun placeCall(): Boolean {
        if (!registrationState.value.isRegistered) {
            return false
        }

        val target = PhoneNumberSanitizer.sanitizeDestination(_uiState.value.inputNumber) ?: return false

        val isTest = target == "3200" || target == "444"
        val balance = userProfile.value.creditBalance

        if (balance <= 0.0 && !isTest) {
            _uiState.value = _uiState.value.copy(showZeroBalanceWarning = true)
            return false
        }

        val callerId = userProfile.value.selectedCallerId
        val country = _uiState.value.detectedCountry.name
        val rate = _uiState.value.estimatedRate

        return callManager.startCall(
            destinationNumber = target,
            callerId = callerId,
            countryName = country,
            rate = rate
        )
    }

    fun endCall() {
        callManager.endCall()
    }

    fun toggleMute() {
        callManager.toggleMute()
    }

    fun toggleSpeaker() {
        callManager.toggleSpeaker()
    }

    fun sendDtmf(digit: Char) {
        callManager.sendDtmfTone(digit)
    }

    fun dismissZeroBalanceWarning() {
        _uiState.value = _uiState.value.copy(showZeroBalanceWarning = false)
    }
}
