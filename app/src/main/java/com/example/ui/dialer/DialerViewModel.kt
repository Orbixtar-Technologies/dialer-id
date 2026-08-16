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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class DialerUiState(
    val inputNumber: String = "",
    val formattedDisplay: String = "",
    val detectedCountry: CountryInfo = CountryUtils.COUNTRIES.first(),
    val estimatedRate: Double = 0.015,
    val showZeroBalanceWarning: Boolean = false
)

/**
 * A distinct destination derived from the persisted Room call log,
 * used by the dialer quick-dial row.
 */
data class RecentDestination(
    val number: String,
    val lastCalledAt: Long,
    val callCount: Int
)

class DialerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DialerRepository.getInstance(application)
    private val callManager = CallManager.getInstance(application)

    val userProfile: StateFlow<UserProfile> = repository.userProfile

    val activeCallState: StateFlow<ActiveCallInfo> = callManager.callState
    val registrationState = callManager.sipEngine.registrationState

    val callerIds: StateFlow<List<CallerIdItem>> = repository.allCallerIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Most recently dialed distinct numbers, newest first, from the local call log. */
    val recentDestinations: StateFlow<List<RecentDestination>> = repository.allCallLogs
        .map { logs ->
            logs.asSequence()
                .filter { it.destinationNumber.isNotBlank() }
                .groupBy { it.destinationNumber }
                .map { (number, entries) ->
                    RecentDestination(
                        number = number,
                        lastCalledAt = entries.maxOf { it.timestamp },
                        callCount = entries.size
                    )
                }
                .sortedByDescending { it.lastCalledAt }
                .take(MAX_RECENT_DESTINATIONS)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(DialerUiState())
    val uiState: StateFlow<DialerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userProfile.collect { profile ->
                if (!profile.isGuest && profile.sipConfig?.hasUsableCredentials() == true) {
                    SipRegisterService.start(getApplication())
                }
            }
        }
        viewModelScope.launch {
            while (isActive) {
                refreshRegistrationIfStale()
                kotlinx.coroutines.delay(30_000L)
            }
        }
    }

    fun onScreenOpened() {
        refreshRegistrationIfStale()
    }

    /** Re-sends a REGISTER for the "Retry" affordance on the status pill. */
    fun retryRegistration() {
        refreshRegistrationIfStale()
    }

    private fun refreshRegistrationIfStale() {
        val profile = userProfile.value
        val state = registrationState.value
        if (!profile.isGuest &&
            profile.sipConfig?.hasUsableCredentials() == true &&
            !state.isRegistered &&
            state.status != com.example.service.sip.RegistrationStatus.REGISTERING &&
            state.status != com.example.service.sip.RegistrationStatus.AUTHENTICATING
        ) {
            SipRegisterService.refresh(getApplication())
        }
    }

    /**
     * Promotes [item] to the primary caller ID so it becomes
     * [UserProfile.selectedCallerId] for the next outbound call.
     */
    fun selectCallerId(item: CallerIdItem) {
        viewModelScope.launch {
            repository.setPrimaryCallerId(item.id, item.phoneNumber)
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

    /**
     * Dials [number] straight away, e.g. from a contact row, applying the same
     * registration and balance gates as the keypad call button.
     */
    fun placeCallTo(number: String): Boolean {
        updateNumber(number)
        return placeCall()
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

    private companion object {
        const val MAX_RECENT_DESTINATIONS = 8
    }
}
