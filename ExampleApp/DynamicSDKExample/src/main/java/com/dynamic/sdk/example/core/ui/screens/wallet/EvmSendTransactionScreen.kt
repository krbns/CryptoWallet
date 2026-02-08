package com.dynamic.sdk.example.core.ui.screens.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dynamic.sdk.android.DynamicSDK
import com.dynamic.sdk.android.Models.BaseWallet
import com.dynamic.sdk.android.Chains.EVM.EthereumTransaction
import com.dynamic.sdk.android.Chains.EVM.convertEthToWei
import com.dynamic.sdk.example.core.ui.components.ErrorMessageView
import com.dynamic.sdk.example.core.ui.components.InfoCard
import com.dynamic.sdk.example.core.ui.components.PrimaryButton
import com.dynamic.sdk.example.core.ui.components.SuccessMessageView
import com.dynamic.sdk.example.core.ui.components.TextFieldWithLabel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.contentOrNull
import java.math.BigInteger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvmSendTransactionScreen(
    onNavigateBack: () -> Unit,
    wallet: BaseWallet
) {
    val viewModel: EvmSendTransactionViewModel = viewModel(
        factory = EvmSendTransactionViewModelFactory(wallet)
    )
    val recipientAddress by viewModel.recipientAddress.collectAsState()
    val amount by viewModel.amount.collectAsState()
    val transactionHash by viewModel.transactionHash.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Send Transaction",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Recipient Address
            TextFieldWithLabel(
                label = "Recipient Address",
                placeholder = "0x742d35Cc6634C0532925a3b844Bc9e7595f0bDd7",
                value = recipientAddress,
                onValueChange = { viewModel.updateRecipientAddress(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Amount
            TextFieldWithLabel(
                label = "Amount (ETH)",
                placeholder = "0.001",
                value = amount,
                onValueChange = { viewModel.updateAmount(it) },
                keyboardType = KeyboardType.Decimal
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Error Message
            errorMessage?.let { error ->
                ErrorMessageView(message = error)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Send Button
            PrimaryButton(
                title = "Send Transaction",
                onClick = { viewModel.sendTransaction() },
                isLoading = isLoading,
                isDisabled = !viewModel.isFormValid()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Transaction Result
            transactionHash?.let { txHash ->
                InfoCard(
                    title = "Transaction Hash",
                    content = txHash
                )
                Spacer(modifier = Modifier.height(8.dp))
                SuccessMessageView(message = "Transaction sent successfully!")
            }
        }
    }
}

class EvmSendTransactionViewModel(private val wallet: BaseWallet) : ViewModel() {
    private val sdk = DynamicSDK.getInstance()

    private val _recipientAddress = MutableStateFlow("")
    val recipientAddress: StateFlow<String> = _recipientAddress.asStateFlow()

    private val _amount = MutableStateFlow("0.001")
    val amount: StateFlow<String> = _amount.asStateFlow()

    private val _transactionHash = MutableStateFlow<String?>(null)
    val transactionHash: StateFlow<String?> = _transactionHash.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun updateRecipientAddress(value: String) { _recipientAddress.value = value }
    fun updateAmount(value: String) { _amount.value = value }

    fun isFormValid(): Boolean {
        return _recipientAddress.value.isNotEmpty() &&
                _amount.value.isNotEmpty() &&
                _amount.value.toDoubleOrNull() != null &&
                _recipientAddress.value.startsWith("0x")
    }

    fun sendTransaction() {
        val amountValue = _amount.value.replace(",", ".").toDoubleOrNull()
        if (amountValue == null) {
            _errorMessage.value = "Invalid amount format"
            return
        }

        if (!_recipientAddress.value.startsWith("0x")) {
            _errorMessage.value = "Invalid recipient address format"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _transactionHash.value = null

            try {
                // Get chain ID from network
                val chainId = resolveChainId()

                // Create EVM client
                val client = sdk.evm.createPublicClient(chainId)

                // Get gas price
                val gasPrice = client.getGasPrice()

                // Calculate max fee per gas (2x gas price for EIP-1559)
                val maxFeePerGas = gasPrice * BigInteger.valueOf(2)
                val maxPriorityFeePerGas = gasPrice

                // Convert amount to wei
                val weiAmount = convertEthToWei(_amount.value)

                // Create transaction
                val transaction = EthereumTransaction(
                    from = wallet.address,
                    to = _recipientAddress.value,
                    value = weiAmount,
                    gas = BigInteger.valueOf(21000), // Standard gas limit for ETH transfer
                    maxFeePerGas = maxFeePerGas,
                    maxPriorityFeePerGas = maxPriorityFeePerGas
                )

                // Send transaction
                val txHash = sdk.evm.sendTransaction(transaction, wallet)
                _transactionHash.value = txHash

            } catch (e: Exception) {
                _errorMessage.value = (e.message + e.stackTrace.toString())
            }

            _isLoading.value = false
        }
    }

    private suspend fun resolveChainId(): Int {
        // Try to get from wallet's current network
        try {
            val network = sdk.wallets.getNetwork(wallet)
            val jsonValue = network.value
            if (jsonValue is JsonPrimitive) {
                jsonValue.intOrNull?.let { return it }
                jsonValue.contentOrNull?.toIntOrNull()?.let { return it }
            }
        } catch (e: Exception) {
            // Ignore
        }

        // Fall back to first EVM network or default to Ethereum mainnet
        return 1
    }
}

class EvmSendTransactionViewModelFactory(private val wallet: BaseWallet) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EvmSendTransactionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EvmSendTransactionViewModel(wallet) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
