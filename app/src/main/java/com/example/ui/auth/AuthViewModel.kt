package com.example.ui.auth

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.FirebaseAuthService
import com.example.data.repository.DialerRepository
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val emailInput: String = "",
    val passwordInput: String = "",
    val displayNameInput: String = "",
    val isSignUpMode: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isAuthenticated: Boolean = false
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authService = FirebaseAuthService(application)
    private val repository = DialerRepository.getInstance(application)

    private val _uiState = MutableStateFlow(
        AuthUiState(isAuthenticated = authService.currentUser != null)
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        val initialUser = authService.currentUser
        if (initialUser != null) {
            repository.bindUser(initialUser)
            com.example.service.SipRegisterService.start(getApplication())
        }
        // Observe Firebase Auth state continuously
        viewModelScope.launch {
            authService.authStateFlow.collect { user ->
                if (user != null) {
                    _uiState.value = _uiState.value.copy(
                        isAuthenticated = true,
                        isLoading = false,
                        errorMessage = null
                    )
                    repository.bindUser(user)
                    com.example.service.SipRegisterService.start(getApplication())
                }
            }
        }
    }

    fun onEmailChanged(email: String) {
        _uiState.value = _uiState.value.copy(emailInput = email, errorMessage = null)
    }

    fun onPasswordChanged(password: String) {
        _uiState.value = _uiState.value.copy(passwordInput = password, errorMessage = null)
    }

    fun onDisplayNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(displayNameInput = name, errorMessage = null)
    }

    fun toggleAuthMode() {
        _uiState.value = _uiState.value.copy(
            isSignUpMode = !_uiState.value.isSignUpMode,
            errorMessage = null,
            successMessage = null
        )
    }

    fun signInWithGoogle(activity: Activity) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            val result = authService.signInWithGoogle(activity)
            result.onSuccess { user ->
                repository.bindUser(user)
                com.example.service.SipRegisterService.start(getApplication())
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isAuthenticated = true,
                    errorMessage = null
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.localizedMessage ?: "Google sign-in was uncompleted."
                )
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun mapFirebaseError(throwable: Throwable?, isSignUp: Boolean): String {
        return when (throwable) {
            is FirebaseAuthInvalidCredentialsException -> {
                val message = throwable.message.orEmpty().lowercase()
                if (message.contains("badly formatted") || message.contains("invalid email")) {
                    "Please enter a valid email address (e.g. name@example.com)."
                } else if (message.contains("credential is incorrect") || message.contains("password is invalid") || message.contains("expired")) {
                    "Incorrect password or account credentials. Please check and try again."
                } else {
                    "Invalid sign-in credentials. Please check your email and password."
                }
            }
            is FirebaseAuthInvalidUserException -> {
                "No account found with this email. Please create an account first."
            }
            is FirebaseAuthUserCollisionException -> {
                "An account with this email address already exists. Please switch to Sign In."
            }
            is FirebaseAuthWeakPasswordException -> {
                "Password is too weak. Please use at least 6 characters."
            }
            is FirebaseAuthException -> {
                throwable.localizedMessage ?: if (isSignUp) "Account registration failed." else "Sign-in failed."
            }
            else -> {
                val msg = throwable?.localizedMessage.orEmpty()
                if (msg.contains("badly formatted", ignoreCase = true)) {
                    "Please enter a valid email address."
                } else if (msg.contains("auth credential", ignoreCase = true)) {
                    "Incorrect email or password."
                } else if (msg.isNotBlank()) {
                    msg
                } else {
                    if (isSignUp) "Account creation failed." else "Sign-in failed. Please check your credentials."
                }
            }
        }
    }

    fun submitEmailAuth() {
        val email = _uiState.value.emailInput.trim()
        val pass = _uiState.value.passwordInput

        if (email.isEmpty() || pass.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter both email and password.")
            return
        }

        if (!isValidEmail(email)) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a valid email address (e.g. name@example.com).")
            return
        }

        if (pass.length < 6) {
            _uiState.value = _uiState.value.copy(errorMessage = "Password must be at least 6 characters.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            if (_uiState.value.isSignUpMode) {
                val displayName = _uiState.value.displayNameInput.trim().ifEmpty {
                    email.substringBefore("@").replaceFirstChar { it.uppercase() }
                }
                val result = authService.signUpWithEmail(email, pass, displayName)
                result.onSuccess { user ->
                    repository.bindUser(user)
                    com.example.service.SipRegisterService.start(getApplication())
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        errorMessage = null
                    )
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = mapFirebaseError(error, isSignUp = true)
                    )
                }
            } else {
                val result = authService.signInWithEmail(email, pass)
                result.onSuccess { user ->
                    repository.bindUser(user)
                    com.example.service.SipRegisterService.start(getApplication())
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        errorMessage = null
                    )
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = mapFirebaseError(error, isSignUp = false)
                    )
                }
            }
        }
    }

    fun loginAsGuest() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            repository.unbindUser()
            com.example.service.SipRegisterService.stop(getApplication())
            _uiState.value = _uiState.value.copy(isLoading = false, isAuthenticated = true)
        }
    }

    fun logout() {
        viewModelScope.launch {
            authService.signOut()
            repository.unbindUser()
            com.example.service.SipRegisterService.stop(getApplication())
            _uiState.value = AuthUiState(
                isAuthenticated = false,
                emailInput = "",
                passwordInput = "",
                displayNameInput = ""
            )
        }
    }
}
