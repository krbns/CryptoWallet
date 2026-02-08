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
import com.dynamic.sdk.android.Chains.EVM.signEthereumTransaction
import com.dynamic.sdk.example.core.ui.components.ErrorMessageView
import com.dynamic.sdk.example.core.ui.components.InfoCard
import com.dynamic.sdk.example.core.ui.components.PrimaryButton
import com.dynamic.sdk.example.core.ui.components.SuccessMessageView
import com.dynamic.sdk.example.core.ui.components.TextFieldWithLabel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvmSignTransactionScreen(
    onNavigateBack: () -> Unit,
    wallet: BaseWallet
) {
    val viewModel: EvmSignTransactionViewModel = viewModel(
        factory = EvmSignTransactionViewModelFactory(wallet)
    )
    val toAddress by viewModel.toAddress.collectAsState()
    val value by viewModel.value.collectAsState()
    val gasLimit by viewModel.gasLimit.collectAsState()
    val maxPriorityFeePerGas by viewModel.maxPriorityFeePerGas.collectAsState()
    val maxFeePerGas by viewModel.maxFeePerGas.collectAsState()
    val signedTx by viewModel.signedTx.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Sign Transaction",
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
        // Info Card
            InfoCard(
                title = "Sign EVM Transaction",
                content = "Sign an EVM transaction without sending it to the network.",
                copyable = false
            )

        Spacer(modifier = Modifier.height(20.dp))

        // To Address
            TextFieldWithLabel(
                label = "To Address",
                placeholder = "0x...",
                value = toAddress,
                onValueChange = { viewModel.updateToAddress(it) }
            )

        Spacer(modifier = Modifier.height(16.dp))

        // Value
            TextFieldWithLabel(
                label = "Value (ETH)",
                placeholder = "0.001",
                value = value,
                onValueChange = { viewModel.updateValue(it) },
                keyboardType = KeyboardType.Decimal
            )

        Spacer(modifier = Modifier.height(16.dp))

        // Gas Limit
            TextFieldWithLabel(
                label = "Gas Limit",
                placeholder = "21000",
                value = gasLimit,
                onValueChange = { viewModel.updateGasLimit(it) },
                keyboardType = KeyboardType.Number
            )

        Spacer(modifier = Modifier.height(16.dp))

        // Max Priority Fee Per Gas
            TextFieldWithLabel(
                label = "Max Priority Fee (Gwei)",
                placeholder = "2",
                value = maxPriorityFeePerGas,
                onValueChange = { viewModel.updateMaxPriorityFeePerGas(it) },
                keyboardType = KeyboardType.Decimal
            )

        Spacer(modifier = Modifier.height(16.dp))

        // Max Fee Per Gas
            TextFieldWithLabel(
                label = "Max Fee Per Gas (Gwei)",
                placeholder = "50",
                value = maxFeePerGas,
                onValueChange = { viewModel.updateMaxFeePerGas(it) },
                keyboardType = KeyboardType.Decimal
            )

        Spacer(modifier = Modifier.height(20.dp))

        // Sign Button
            PrimaryButton(
                title = if (isLoading) "Signing..." else "Sign Transaction",
                onClick = { viewModel.signTransaction() },
                isLoading = isLoading
            )

        Spacer(modifier = Modifier.height(16.dp))

        // Error Message
        errorMessage?.let { error ->
            ErrorMessageView(message = error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Success Message
        signedTx?.let { signed ->
            SuccessMessageView(message = signed)
        }
        }
    }
}

class EvmSignTransactionViewModel(private val wallet: BaseWallet) : ViewModel() {
    private val sdk = DynamicSDK.getInstance()

    private val _toAddress = MutableStateFlow("")
    val toAddress: StateFlow<String> = _toAddress.asStateFlow()

    private val _value = MutableStateFlow("0.001")
    val value: StateFlow<String> = _value.asStateFlow()

    private val _gasLimit = MutableStateFlow("21000")
    val gasLimit: StateFlow<String> = _gasLimit.asStateFlow()

    private val _maxPriorityFeePerGas = MutableStateFlow("2")
    val maxPriorityFeePerGas: StateFlow<String> = _maxPriorityFeePerGas.asStateFlow()

    private val _maxFeePerGas = MutableStateFlow("50")
    val maxFeePerGas: StateFlow<String> = _maxFeePerGas.asStateFlow()

    private val _signedTx = MutableStateFlow<String?>(null)
    val signedTx: StateFlow<String?> = _signedTx.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun updateToAddress(value: String) { _toAddress.value = value }
    fun updateValue(value: String) { _value.value = value }
    fun updateGasLimit(value: String) { _gasLimit.value = value }
    fun updateMaxPriorityFeePerGas(value: String) { _maxPriorityFeePerGas.value = value }
    fun updateMaxFeePerGas(value: String) { _maxFeePerGas.value = value }

    fun signTransaction() {
        if (_toAddress.value.isEmpty()) {
            _errorMessage.value = "Please enter a recipient address"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _signedTx.value = null

            try {
                val signed = sdk.wallets.signEthereumTransaction(
                    wallet = wallet,
                    to = _toAddress.value,
                    value = _value.value,
                    gasLimit = _gasLimit.value,
                    maxPriorityFeePerGas = _maxPriorityFeePerGas.value,
                    maxFeePerGas = _maxFeePerGas.value
                )
                _signedTx.value = signed
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to sign transaction"
            }

            _isLoading.value = false
        }
    }
}

class EvmSignTransactionViewModelFactory(private val wallet: BaseWallet) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EvmSignTransactionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EvmSignTransactionViewModel(wallet) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
