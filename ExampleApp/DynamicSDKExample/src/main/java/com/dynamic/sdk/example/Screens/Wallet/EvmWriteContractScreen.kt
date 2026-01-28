package com.dynamic.sdk.example.Screens.Wallet

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
import com.dynamic.sdk.android.Chains.EVM.WriteContractInput
import com.dynamic.sdk.example.Components.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private val SAMPLE_ABI = """
[
  {
    "inputs": [
      { "internalType": "address", "name": "to", "type": "address" },
      { "internalType": "uint256", "name": "amount", "type": "uint256" }
    ],
    "name": "transfer",
    "outputs": [{ "internalType": "bool", "name": "", "type": "bool" }],
    "stateMutability": "nonpayable",
    "type": "function"
  }
]
""".trimIndent()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvmWriteContractScreen(
    onNavigateBack: () -> Unit,
    wallet: BaseWallet
) {
    val viewModel: EvmWriteContractViewModel = viewModel(
        factory = EvmWriteContractViewModelFactory(wallet)
    )
    val contractAddress by viewModel.contractAddress.collectAsState()
    val functionName by viewModel.functionName.collectAsState()
    val abiJson by viewModel.abiJson.collectAsState()
    val argsJson by viewModel.argsJson.collectAsState()
    val transactionHash by viewModel.transactionHash.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Write Contract",
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
            title = "Write Contract",
            content = "Execute a write operation on a smart contract.",
            copyable = false
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Contract Address
        TextFieldWithLabel(
            label = "Contract Address",
            placeholder = "0x...",
            value = contractAddress,
            onValueChange = { viewModel.updateContractAddress(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Function Name
        TextFieldWithLabel(
            label = "Function Name",
            placeholder = "transfer",
            value = functionName,
            onValueChange = { viewModel.updateFunctionName(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ABI JSON
        TextFieldWithLabel(
            label = "ABI JSON",
            placeholder = "Enter contract ABI",
            value = abiJson,
            onValueChange = { viewModel.updateAbiJson(it) },
            singleLine = false
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Args JSON
        TextFieldWithLabel(
            label = "Arguments (JSON array)",
            placeholder = "[\"0x...\", \"1000000000000000000\"]",
            value = argsJson,
            onValueChange = { viewModel.updateArgsJson(it) }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Write Button
        PrimaryButton(
            title = if (isLoading) "Writing..." else "Write Contract",
            onClick = { viewModel.writeContract() },
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
            SuccessMessageView(message = "Contract write successful!")
        }
        }
    }
}

class EvmWriteContractViewModel(private val wallet: BaseWallet) : ViewModel() {
    private val sdk = DynamicSDK.getInstance()

    private val _contractAddress = MutableStateFlow("")
    val contractAddress: StateFlow<String> = _contractAddress.asStateFlow()

    private val _functionName = MutableStateFlow("transfer")
    val functionName: StateFlow<String> = _functionName.asStateFlow()

    private val _abiJson = MutableStateFlow(SAMPLE_ABI)
    val abiJson: StateFlow<String> = _abiJson.asStateFlow()

    private val _argsJson = MutableStateFlow("[]")
    val argsJson: StateFlow<String> = _argsJson.asStateFlow()

    private val _transactionHash = MutableStateFlow<String?>(null)
    val transactionHash: StateFlow<String?> = _transactionHash.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun updateContractAddress(value: String) { _contractAddress.value = value }
    fun updateFunctionName(value: String) { _functionName.value = value }
    fun updateAbiJson(value: String) { _abiJson.value = value }
    fun updateArgsJson(value: String) { _argsJson.value = value }

    @Suppress("UNCHECKED_CAST")
    fun writeContract() {
        if (_contractAddress.value.isEmpty()) {
            _errorMessage.value = "Please enter a contract address"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _transactionHash.value = null

            try {
                // Parse ABI using org.json
                val abiList = parseAbiJson(_abiJson.value)

                // Parse args
                val argsList = parseArgsJson(_argsJson.value)

                val input = WriteContractInput(
                    address = _contractAddress.value,
                    abi = abiList,
                    functionName = _functionName.value,
                    args = argsList
                )

                val txHash = sdk.evm.writeContract(wallet, input)
                _transactionHash.value = txHash

            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to write contract"
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

    private fun parseArgsJson(argsString: String): List<Any> {
        val jsonArray = JSONArray(argsString)
        val result = mutableListOf<Any>()

        for (i in 0 until jsonArray.length()) {
            result.add(jsonValueToAny(jsonArray.get(i)))
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

class EvmWriteContractViewModelFactory(private val wallet: BaseWallet) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EvmWriteContractViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EvmWriteContractViewModel(wallet) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
