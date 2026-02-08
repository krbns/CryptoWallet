package com.dynamic.sdk.example.feature.send.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dynamic.sdk.example.core.data.repository.TransactionRepository
import com.dynamic.sdk.example.core.data.repository.WalletRepository
import com.dynamic.sdk.example.core.di.IoDispatcher
import com.dynamic.sdk.example.core.domain.isValidEthAmount
import com.dynamic.sdk.example.core.domain.isValidEvmAddress
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface SendTxStatus {
    data object Idle : SendTxStatus
    data object Loading : SendTxStatus
    data class Success(val txHash: String) : SendTxStatus
    data class Error(val message: String) : SendTxStatus
}

data class SendTransactionUiState(
    val walletAddress: String = "",
    val recipientAddress: String = "",
    val amountEth: String = "",
    val recipientError: String? = null,
    val amountError: String? = null,
    val status: SendTxStatus = SendTxStatus.Idle,
)

@HiltViewModel
class SendTransactionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val walletRepository: WalletRepository,
    private val transactionRepository: TransactionRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val walletAddressArg: String = savedStateHandle.get<String>("address").orEmpty()

    private val _uiState = MutableStateFlow(
        SendTransactionUiState(
            walletAddress = walletAddressArg
        )
    )
    val uiState: StateFlow<SendTransactionUiState> = _uiState.asStateFlow()

    fun updateRecipientAddress(value: String) {
        _uiState.value = _uiState.value.copy(
            recipientAddress = value,
            recipientError = null,
            status = SendTxStatus.Idle
        )
    }

    fun updateAmountEth(value: String) {
        _uiState.value = _uiState.value.copy(
            amountEth = value,
            amountError = null,
            status = SendTxStatus.Idle
        )
    }

    fun isFormValid(): Boolean {
        val state = _uiState.value
        return isValidEvmAddress(state.recipientAddress) && isValidEthAmount(state.amountEth)
    }

    fun sendTransaction() {
        val state = _uiState.value
        val recipient = state.recipientAddress.trim()
        val amount = state.amountEth.trim()

        val recipientError = if (isValidEvmAddress(recipient)) null else "Invalid recipient address"
        val amountError = if (isValidEthAmount(amount)) null else "Invalid amount"

        if (recipientError != null || amountError != null) {
            _uiState.value = state.copy(
                recipientError = recipientError,
                amountError = amountError
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(status = SendTxStatus.Loading)
            try {
                val wallet = withContext(ioDispatcher) {
                    walletRepository.getEvmWalletByAddress(walletAddressArg)
                } ?: throw IllegalStateException("Wallet not found")

                var chainId = withContext(ioDispatcher) {
                    walletRepository.getNetworkChainId(wallet)
                }
                if (chainId != SEPOLIA_CHAIN_ID) {
                    withContext(ioDispatcher) { walletRepository.switchToSepolia(wallet) }
                    chainId = withContext(ioDispatcher) { walletRepository.getNetworkChainId(wallet) }
                }
                if (chainId != SEPOLIA_CHAIN_ID) {
                    throw IllegalStateException("Failed to switch to Sepolia")
                }

                val txHash = withContext(ioDispatcher) {
                    transactionRepository.sendSepoliaTransaction(
                        wallet = wallet,
                        recipientAddress = recipient,
                        amountEth = amount
                    )
                }
                _uiState.value = _uiState.value.copy(status = SendTxStatus.Success(txHash))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    status = SendTxStatus.Error(e.message ?: "Transaction failed")
                )
            }
        }
    }

    companion object {
        private const val SEPOLIA_CHAIN_ID = 11155111
    }
}
