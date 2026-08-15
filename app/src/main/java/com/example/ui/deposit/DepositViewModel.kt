package com.example.ui.deposit

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.repository.DialerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DepositUiState(
    val selectedAmount: Double = 25.00,
    val customAmountInput: String = "",
    val isDepositSuccess: Boolean = false
)

class DepositViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DialerRepository.getInstance(application)
    val userProfile = repository.userProfile

    private val _uiState = MutableStateFlow(DepositUiState())
    val uiState: StateFlow<DepositUiState> = _uiState.asStateFlow()

    fun selectAmount(amount: Double) {
        _uiState.value = _uiState.value.copy(
            selectedAmount = amount,
            customAmountInput = ""
        )
    }

    fun onCustomAmountChanged(input: String) {
        val filtered = input.filter { it.isDigit() || it == '.' }
        val amount = filtered.toDoubleOrNull() ?: 0.0
        _uiState.value = _uiState.value.copy(
            customAmountInput = filtered,
            selectedAmount = amount
        )
    }

    fun simulateInstantTopUp() {
        if (!BuildConfig.DEBUG) return
        val amount = _uiState.value.selectedAmount
        if (amount <= 0.0) return

        viewModelScope.launch {
            repository.addCredit(amount)
            Toast.makeText(getApplication(), "Debug credit +$${String.format("%.2f", amount)}", Toast.LENGTH_SHORT).show()
            _uiState.value = _uiState.value.copy(isDepositSuccess = true)
        }
    }
}
