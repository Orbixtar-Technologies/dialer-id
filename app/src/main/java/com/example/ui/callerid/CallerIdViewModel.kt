package com.example.ui.callerid

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CallerIdItem
import com.example.data.repository.DialerRepository
import com.example.util.PhoneNumberSanitizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CallerIdUiState(
    val showAddDialog: Boolean = false,
    val inputPhoneNumber: String = "",
    val inputLabel: String = "",
    val isPrimaryToggle: Boolean = false,
    val errorMessage: String? = null
)

class CallerIdViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DialerRepository.getInstance(application)

    val callerIds: StateFlow<List<CallerIdItem>> = repository.allCallerIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile = repository.userProfile

    private val _uiState = MutableStateFlow(CallerIdUiState())
    val uiState: StateFlow<CallerIdUiState> = _uiState.asStateFlow()

    fun showAddDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(
            showAddDialog = show,
            inputPhoneNumber = "",
            inputLabel = "",
            isPrimaryToggle = false,
            errorMessage = null
        )
    }

    fun onPhoneNumberChanged(number: String) {
        _uiState.value = _uiState.value.copy(
            inputPhoneNumber = PhoneNumberSanitizer.filterDialInput(number),
            errorMessage = null
        )
    }

    fun onLabelChanged(label: String) {
        _uiState.value = _uiState.value.copy(inputLabel = label, errorMessage = null)
    }

    fun onPrimaryToggleChanged(isPrimary: Boolean) {
        _uiState.value = _uiState.value.copy(isPrimaryToggle = isPrimary)
    }

    fun addCallerId() {
        val number = _uiState.value.inputPhoneNumber.trim()
        val label = _uiState.value.inputLabel.trim()

        if (!PhoneNumberSanitizer.isValidCallerId(number)) {
            _uiState.value = _uiState.value.copy(errorMessage = "Enter a phone number using only + and digits (6+ digits).")
            return
        }

        viewModelScope.launch {
            repository.addCallerId(
                phoneNumber = number,
                label = label.ifEmpty { "Caller ID" },
                isPrimary = _uiState.value.isPrimaryToggle
            )
            showAddDialog(false)
        }
    }

    fun setPrimary(item: CallerIdItem) {
        viewModelScope.launch {
            repository.setPrimaryCallerId(item.id, item.phoneNumber)
        }
    }

    fun deleteCallerId(item: CallerIdItem) {
        viewModelScope.launch {
            repository.deleteCallerId(item)
        }
    }
}
