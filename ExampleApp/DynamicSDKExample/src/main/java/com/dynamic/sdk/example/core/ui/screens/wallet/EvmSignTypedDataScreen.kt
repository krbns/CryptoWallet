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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dynamic.sdk.android.DynamicSDK
import com.dynamic.sdk.android.Models.BaseWallet
import com.dynamic.sdk.android.Chains.EVM.signTypedData
import com.dynamic.sdk.example.core.ui.components.ErrorMessageView
import com.dynamic.sdk.example.core.ui.components.InfoCard
import com.dynamic.sdk.example.core.ui.components.PrimaryButton
import com.dynamic.sdk.example.core.ui.components.SuccessMessageView
import com.dynamic.sdk.example.core.ui.components.TextFieldWithLabel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private val SAMPLE_TYPED_DATA = """
{
  "types": {
    "EIP712Domain": [
      { "name": "name", "type": "string" },
      { "name": "version", "type": "string" },
      { "name": "chainId", "type": "uint256" },
      { "name": "verifyingContract", "type": "address" }
    ],
    "Person": [
      { "name": "name", "type": "string" },
      { "name": "wallet", "type": "address" }
    ],
    "Mail": [
      { "name": "from", "type": "Person" },
      { "name": "to", "type": "Person" },
      { "name": "contents", "type": "string" }
    ]
  },
  "primaryType": "Mail",
  "domain": {
    "name": "Ether Mail",
    "version": "1",
    "chainId": 1,
    "verifyingContract": "0xCcCCccccCCCCcCCCCCCcCcCccCcCCCcCcccccccC"
  },
  "message": {
    "from": {
      "name": "Alice",
      "wallet": "0xCD2a3d9F938E13CD947Ec05AbC7FE734Df8DD826"
    },
    "to": {
      "name": "Bob",
      "wallet": "0xbBbBBBBbbBBBbbbBbbBbbbbBBbBbbbbBbBbbBBbB"
    },
    "contents": "Hello, Bob!"
  }
}
""".trimIndent()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvmSignTypedDataScreen(
    onNavigateBack: () -> Unit,
    wallet: BaseWallet
) {
    val viewModel: EvmSignTypedDataViewModel = viewModel(
        factory = EvmSignTypedDataViewModelFactory(wallet)
    )
    val typedDataJson by viewModel.typedDataJson.collectAsState()
    val signature by viewModel.signature.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Sign Typed Data",
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
                title = "Sign Typed Data (EIP-712)",
                content = "Sign structured data following the EIP-712 standard for typed data signing.",
                copyable = false
            )

        Spacer(modifier = Modifier.height(20.dp))

        // Typed Data JSON
            TextFieldWithLabel(
                label = "Typed Data JSON",
                placeholder = "Enter EIP-712 typed data JSON",
                value = typedDataJson,
                onValueChange = { viewModel.updateTypedDataJson(it) },
                singleLine = false
            )

        Spacer(modifier = Modifier.height(20.dp))

        // Sign Button
            PrimaryButton(
                title = if (isLoading) "Signing..." else "Sign Typed Data",
                onClick = { viewModel.signTypedData() },
                isLoading = isLoading
            )

        Spacer(modifier = Modifier.height(16.dp))

        // Error Message
        errorMessage?.let { error ->
            ErrorMessageView(message = error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Success Message
        signature?.let { sig ->
            SuccessMessageView(message = sig)
        }
        }
    }
}

class EvmSignTypedDataViewModel(private val wallet: BaseWallet) : ViewModel() {
    private val sdk = DynamicSDK.getInstance()

    private val _typedDataJson = MutableStateFlow(SAMPLE_TYPED_DATA)
    val typedDataJson: StateFlow<String> = _typedDataJson.asStateFlow()

    private val _signature = MutableStateFlow<String?>(null)
    val signature: StateFlow<String?> = _signature.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun updateTypedDataJson(value: String) { _typedDataJson.value = value }

    fun signTypedData() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _signature.value = null

            try {
                val sig = sdk.wallets.signTypedData(wallet, _typedDataJson.value)
                _signature.value = sig
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to sign typed data"
            }

            _isLoading.value = false
        }
    }
}

class EvmSignTypedDataViewModelFactory(private val wallet: BaseWallet) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EvmSignTypedDataViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EvmSignTypedDataViewModel(wallet) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
