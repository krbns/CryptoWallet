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
import com.dynamic.sdk.android.Chains.EVM.WriteContractInput
import com.dynamic.sdk.android.Chains.EVM.Erc20
import com.dynamic.sdk.android.core.Logger
import com.dynamic.sdk.example.core.ui.components.ErrorMessageView
import com.dynamic.sdk.example.core.ui.components.InfoCard
import com.dynamic.sdk.example.core.ui.components.PrimaryButton
import com.dynamic.sdk.example.core.ui.components.SuccessMessageView
import com.dynamic.sdk.example.core.ui.components.TextFieldWithLabel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvmSendErc20Screen(
    onNavigateBack: () -> Unit,
    wallet: BaseWallet
) {
    val viewModel: EvmSendErc20ViewModel = viewModel(
        factory = EvmSendErc20ViewModelFactory(wallet)
    )
    val tokenAddress by viewModel.tokenAddress.collectAsState()
    val recipientAddress by viewModel.recipientAddress.collectAsState()
    val amount by viewModel.amount.collectAsState()
    val decimals by viewModel.decimals.collectAsState()
    val transactionHash by viewModel.transactionHash.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Send ERC20",
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
                title = "Send ERC20 Token",
                content = "Transfer ERC20 tokens to another address using the standard transfer function.",
                copyable = false
            )

        Spacer(modifier = Modifier.height(20.dp))

        // Token Address
            TextFieldWithLabel(
                label = "Token Contract Address",
                placeholder = "0x...",
                value = tokenAddress,
                onValueChange = { viewModel.updateTokenAddress(it) }
            )

        Spacer(modifier = Modifier.height(16.dp))

        // Recipient Address
            TextFieldWithLabel(
                label = "Recipient Address",
                placeholder = "0x...",
                value = recipientAddress,
                onValueChange = { viewModel.updateRecipientAddress(it) }
            )

        Spacer(modifier = Modifier.height(16.dp))

        // Amount
            TextFieldWithLabel(
                label = "Amount",
                placeholder = "100",
                value = amount,
                onValueChange = { viewModel.updateAmount(it) },
                keyboardType = KeyboardType.Decimal
            )

        Spacer(modifier = Modifier.height(16.dp))

        // Decimals
            TextFieldWithLabel(
                label = "Token Decimals",
                placeholder = "18",
                value = decimals,
                onValueChange = { viewModel.updateDecimals(it) },
                keyboardType = KeyboardType.Number
            )

        Spacer(modifier = Modifier.height(20.dp))

        // Send Button
            PrimaryButton(
                title = if (isLoading) "Sending..." else "Send ERC20",
                onClick = { viewModel.sendErc20() },
                isLoading = isLoading
            )

        Spacer(modifier = Modifier.height(16.dp))

        // Error Message
        errorMessage?.let { error ->
            ErrorMessageView(message = error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Success Message
        transactionHash?.let { txHash ->
            InfoCard(
                title = "Transaction Hash",
                content = txHash
            )
            Spacer(modifier = Modifier.height(8.dp))
            SuccessMessageView(message = "ERC20 transfer successful!")
        }
        }
    }
}

class EvmSendErc20ViewModel(private val wallet: BaseWallet) : ViewModel() {
    private val sdk = DynamicSDK.getInstance()

    private val _tokenAddress = MutableStateFlow("")
    val tokenAddress: StateFlow<String> = _tokenAddress.asStateFlow()

    private val _recipientAddress = MutableStateFlow("")
    val recipientAddress: StateFlow<String> = _recipientAddress.asStateFlow()

    private val _amount = MutableStateFlow("100")
    val amount: StateFlow<String> = _amount.asStateFlow()

    private val _decimals = MutableStateFlow("18")
    val decimals: StateFlow<String> = _decimals.asStateFlow()

    private val _transactionHash = MutableStateFlow<String?>(null)
    val transactionHash: StateFlow<String?> = _transactionHash.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun updateTokenAddress(value: String) { _tokenAddress.value = value }
    fun updateRecipientAddress(value: String) { _recipientAddress.value = value }
    fun updateAmount(value: String) { _amount.value = value }
    fun updateDecimals(value: String) { _decimals.value = value }

    @Suppress("UNCHECKED_CAST")
    fun sendErc20() {
        if (_tokenAddress.value.isEmpty()) {
            _errorMessage.value = "Please enter a token contract address"
            return
        }
        if (_recipientAddress.value.isEmpty()) {
            _errorMessage.value = "Please enter a recipient address"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _transactionHash.value = null

            try {
                // Parse ERC20 ABI using org.json
                val abiList = parseAbiJson(Erc20.abi)

                // Calculate amount with decimals
                val decimalCount = _decimals.value.toIntOrNull() ?: 18
                val amountValue = BigDecimal(_amount.value.replace(",", "."))
                val multiplier = BigDecimal.TEN.pow(decimalCount)
                val rawAmount = amountValue.multiply(multiplier).toBigInteger()

                val input = WriteContractInput(
                    address = _tokenAddress.value,
                    abi = abiList,
                    functionName = "transfer",
                    args = listOf(_recipientAddress.value, rawAmount.toString())
                )
                Logger.debug { "" + input.toJson() }
                val txHash = sdk.evm.writeContract(wallet, input)
                _transactionHash.value = txHash

            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to send ERC20"
            }

            _isLoading.value = false
        }
    }

    private fun parseAbiJson(abiString: String): List<Map<String, Any>> {
        val jsonArray = JSONArray(abiString)
        val result = mutableListOf<Map<String, Any>>()

        for (i in 0 until jsonArray.length()) {
            val jsonObj = jsonArray.getJSONObject(i)
            result.add(jsonObjectToMap(jsonObj))
        }

        return result
    }

    private fun jsonObjectToMap(jsonObj: JSONObject): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        val keys = jsonObj.keys()

        while (keys.hasNext()) {
            val key = keys.next()
            val value = jsonObj.get(key)
            map[key] = jsonValueToAny(value)
        }

        return map
    }

    private fun jsonValueToAny(value: Any): Any {
        return when (value) {
            is JSONObject -> jsonObjectToMap(value)
            is JSONArray -> {
                val list = mutableListOf<Any>()
                for (i in 0 until value.length()) {
                    list.add(jsonValueToAny(value.get(i)))
                }
                list
            }
            JSONObject.NULL -> "null"
            else -> value
        }
    }
}

class EvmSendErc20ViewModelFactory(private val wallet: BaseWallet) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EvmSendErc20ViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EvmSendErc20ViewModel(wallet) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
