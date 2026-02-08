package com.dynamic.sdk.example.feature.login.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dynamic.sdk.example.core.data.repository.AuthRepository
import com.dynamic.sdk.example.core.domain.isValidEmail
import com.dynamic.sdk.example.core.domain.isValidOtp
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

data class LoginUiState(
    val email: String = "",
    val isSendingOtp: Boolean = false,
    val isOtpSheetVisible: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _navigation = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigation: SharedFlow<Unit> = _navigation.asSharedFlow()

    init {
        observeAuthState()
    }

    fun updateEmail(value: String) {
        _uiState.value = _uiState.value.copy(
            email = value,
            errorMessage = null
        )
    }

    fun dismissOtpSheet() {
        _uiState.value = _uiState.value.copy(isOtpSheetVisible = false)
    }

    fun sendOtp() {
        val email = _uiState.value.email.trim()
        if (!isValidEmail(email)) {
            _uiState.value = _uiState.value.copy(errorMessage = "Enter a valid email address")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSendingOtp = true,
                errorMessage = null
            )
            try {
                withTimeout(30_000) {
                    authRepository.sendEmailOtp(email)
                }
                _uiState.value = _uiState.value.copy(isOtpSheetVisible = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to send OTP"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isSendingOtp = false)
            }
        }
    }

    suspend fun verifyOtp(code: String) {
        if (!isValidOtp(code)) {
            throw IllegalArgumentException("OTP must contain 6 digits")
        }
        withTimeout(30_000) {
            authRepository.verifyEmailOtp(code.trim())
        }
    }

    suspend fun resendOtp() {
        withTimeout(30_000) {
            authRepository.resendEmailOtp()
        }
    }

    private fun observeAuthState() {
        if (!authRepository.currentToken().isNullOrEmpty()) {
            _navigation.tryEmit(Unit)
        }

        viewModelScope.launch {
            authRepository.observeTokenChanges().collect { token ->
                if (!token.isNullOrEmpty()) {
                    _navigation.tryEmit(Unit)
                }
            }
        }
    }
}
