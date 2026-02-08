package com.dynamic.sdk.example.feature.walletdetails.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dynamic.sdk.example.core.data.repository.AuthRepository
import com.dynamic.sdk.example.core.data.repository.WalletRepository
import com.dynamic.sdk.example.core.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class WalletUiState(
    val walletAddress: String? = null,
    val networkLabel: String? = null,
    val balanceLabel: String? = null,
    val isLoadingInitial: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val authRepository: AuthRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _uiState = MutableStateFlow(WalletUiState())
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    private val _navigationToLogin = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigationToLogin: SharedFlow<Unit> = _navigationToLogin.asSharedFlow()

    private var isFirstLoadDone = false

    fun loadIfNeeded() {
        if (isFirstLoadDone) return
        refresh(isManual = false)
    }

    fun refresh(isManual: Boolean = true) {
        viewModelScope.launch {
            isFirstLoadDone = true
            _uiState.value = _uiState.value.copy(
                isLoadingInitial = !isManual,
                isRefreshing = isManual,
                errorMessage = null
            )

            try {
                val wallet = withContext(ioDispatcher) { walletRepository.getPrimaryEvmWallet() }
                    ?: throw IllegalStateException("EVM wallet not found for this user")

                var chainId = withContext(ioDispatcher) { walletRepository.getNetworkChainId(wallet) }
                if (chainId != SEPOLIA_CHAIN_ID) {
                    withContext(ioDispatcher) { walletRepository.switchToSepolia(wallet) }
                    chainId = withContext(ioDispatcher) { walletRepository.getNetworkChainId(wallet) }
                }
                if (chainId != SEPOLIA_CHAIN_ID) {
                    throw IllegalStateException("Failed to switch to Sepolia")
                }

                val balance = withContext(ioDispatcher) { walletRepository.getBalance(wallet) }

                _uiState.value = _uiState.value.copy(
                    walletAddress = wallet.address,
                    networkLabel = "Sepolia ($SEPOLIA_CHAIN_ID)",
                    balanceLabel = balance,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to load wallet details"
                )
            } finally {
                _uiState.value = _uiState.value.copy(
                    isLoadingInitial = false,
                    isRefreshing = false
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                withContext(ioDispatcher) { authRepository.logout() }
                _navigationToLogin.tryEmit(Unit)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Logout failed"
                )
            }
        }
    }

    companion object {
        private const val SEPOLIA_CHAIN_ID = 11155111
    }
}
