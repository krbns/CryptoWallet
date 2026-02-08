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
import com.dynamic.sdk.example.core.ui.components.ErrorMessageView
import com.dynamic.sdk.example.core.ui.components.InfoCard
import com.dynamic.sdk.example.core.ui.components.PrimaryButton
import com.dynamic.sdk.example.core.ui.components.SuccessMessageView
import com.dynamic.sdk.example.core.ui.components.TextFieldWithLabel
import com.solanaweb3.*
import org.sol4k.PublicKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolanaSendTransactionScreen(
    onNavigateBack: () -> Unit,
    wallet: BaseWallet
) {
    val viewModel: SolanaSendTransactionViewModel = viewModel(
        factory = SolanaSendTransactionViewModelFactory(wallet)
    )
    val recipientAddress by viewModel.recipientAddress.collectAsState()
    val amount by viewModel.amount.collectAsState()
    val transactionSignature by viewModel.transactionSignature.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Send SOL",
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
            // Wallet Address Display
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "From",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = wallet.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Recipient Address
            TextFieldWithLabel(
                label = "Recipient Address",
                placeholder = "Enter Solana address",
                value = recipientAddress,
                onValueChange = { viewModel.updateRecipientAddress(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Amount in SOL
            TextFieldWithLabel(
                label = "Amount (SOL)",
                placeholder = "0.001",
                value = amount,
                onValueChange = { viewModel.updateAmount(it) },
                keyboardType = KeyboardType.Decimal
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Lamports helper text
            Text(
                text = "1 SOL = 1,000,000,000 lamports",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Error Message
            errorMessage?.let { error ->
                ErrorMessageView(message = error)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Send Transaction Button
            PrimaryButton(
                title = "Send Transaction",
                onClick = { viewModel.sendTransaction() },
                isLoading = isLoading,
                isDisabled = !viewModel.isFormValid()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Transaction Result
            transactionSignature?.let { signature ->
                InfoCard(
                    title = "Transaction Signature",
                    content = signature
                )
                Spacer(modifier = Modifier.height(8.dp))
                SuccessMessageView(message = "Transaction sent successfully!")

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "View on Solana Explorer: https://explorer.solana.com/tx/$signature?cluster=devnet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

class SolanaSendTransactionViewModel(private val wallet: BaseWallet) : ViewModel() {
    private val sdk = DynamicSDK.getInstance()

    private val _recipientAddress = MutableStateFlow("")
    val recipientAddress: StateFlow<String> = _recipientAddress.asStateFlow()

    private val _amount = MutableStateFlow("0.001")
    val amount: StateFlow<String> = _amount.asStateFlow()

    private val _transactionSignature = MutableStateFlow<String?>(null)
    val transactionSignature: StateFlow<String?> = _transactionSignature.asStateFlow()

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
                _amount.value.toDoubleOrNull()!! > 0
    }

    fun sendTransaction() {
        val amountValue = _amount.value.replace(",", ".").toDoubleOrNull()
        if (amountValue == null || amountValue <= 0) {
            _errorMessage.value = "Invalid amount format"
            return
        }

        if (_recipientAddress.value.isEmpty()) {
            _errorMessage.value = "Recipient address is required"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _transactionSignature.value = null

            try {
                // Use devnet connection (you can make this configurable)
                val connection = Connection(Cluster.DEVNET)

                // Parse public keys
                val fromPubkey = PublicKey(wallet.address)
                val toPubkey = PublicKey(_recipientAddress.value)

                // Convert SOL to lamports (1 SOL = 1,000,000,000 lamports)
                val lamports = (amountValue * 1_000_000_000).toLong()

                // Get latest blockhash
                val blockhash = connection.getLatestBlockhash()

                // Create transfer instruction
                val instruction = SystemProgram.transfer(
                    fromPubkey = fromPubkey,
                    toPubkey = toPubkey,
                    lamports = lamports
                )

                // Create v0 transaction
                val transaction = Transaction.v0(
                    payer = fromPubkey,
                    instructions = listOf(instruction),
                    recentBlockhash = blockhash.blockhash
                )

                val base64 = transaction.serializeUnsignedToBase64()

                // Send transaction through Dynamic SDK
                val signature = sdk.solana.signAndSendTransaction(base64, wallet)
                _transactionSignature.value = signature

            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to send transaction"
                e.printStackTrace()
            }

            _isLoading.value = false
        }
    }
}

class SolanaSendTransactionViewModelFactory(private val wallet: BaseWallet) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SolanaSendTransactionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SolanaSendTransactionViewModel(wallet) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
