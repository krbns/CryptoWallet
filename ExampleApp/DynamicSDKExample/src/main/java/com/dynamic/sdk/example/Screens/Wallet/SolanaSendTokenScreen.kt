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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dynamic.sdk.android.DynamicSDK
import com.dynamic.sdk.android.Models.BaseWallet
import com.dynamic.sdk.android.core.Logger
import com.dynamic.sdk.example.Components.*
import com.solanaweb3.*
import org.sol4k.PublicKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolanaSendTokenScreen(
    onNavigateBack: () -> Unit,
    wallet: BaseWallet
) {
    val viewModel: SolanaSendTokenViewModel = viewModel(
        factory = SolanaSendTokenViewModelFactory(wallet)
    )
    val tokenMintAddress by viewModel.tokenMintAddress.collectAsState()
    val recipientAddress by viewModel.recipientAddress.collectAsState()
    val amount by viewModel.amount.collectAsState()
    val decimals by viewModel.decimals.collectAsState()
    val transactionSignature by viewModel.transactionSignature.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Send SPL Token",
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
                title = "Send SPL Token",
                content = "Transfer SPL tokens (like USDC, USDT) to another Solana address. Enter the token mint address and recipient details.",
                copyable = false
            )

            Spacer(modifier = Modifier.height(20.dp))

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

            // Token Mint Address
            TextFieldWithLabel(
                label = "Token Mint Address",
                placeholder = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v (USDC)",
                value = tokenMintAddress,
                onValueChange = { viewModel.updateTokenMintAddress(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Recipient Address
            TextFieldWithLabel(
                label = "Recipient Address",
                placeholder = "Enter Solana address",
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

            // Token Decimals
            TextFieldWithLabel(
                label = "Token Decimals",
                placeholder = "6 for USDC, 9 for most tokens",
                value = decimals,
                onValueChange = { viewModel.updateDecimals(it) },
                keyboardType = KeyboardType.Number
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Helper text
            Text(
                text = "Common tokens: USDC (6 decimals), USDT (6 decimals), SOL (9 decimals)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Error Message
            errorMessage?.let { error ->
                ErrorMessageView(message = error)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Send Token Transfer Button
            PrimaryButton(
                title = if (isLoading) "Sending..." else "Send Token",
                onClick = { viewModel.sendTokenTransfer() },
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
                SuccessMessageView(message = "Token transfer successful!")

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

class SolanaSendTokenViewModel(private val wallet: BaseWallet) : ViewModel() {
    private val sdk = DynamicSDK.getInstance()

    private val _tokenMintAddress = MutableStateFlow("")
    val tokenMintAddress: StateFlow<String> = _tokenMintAddress.asStateFlow()

    private val _recipientAddress = MutableStateFlow("")
    val recipientAddress: StateFlow<String> = _recipientAddress.asStateFlow()

    private val _amount = MutableStateFlow("100")
    val amount: StateFlow<String> = _amount.asStateFlow()

    private val _decimals = MutableStateFlow("6")
    val decimals: StateFlow<String> = _decimals.asStateFlow()

    private val _transactionSignature = MutableStateFlow<String?>(null)
    val transactionSignature: StateFlow<String?> = _transactionSignature.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun updateTokenMintAddress(value: String) { _tokenMintAddress.value = value }
    fun updateRecipientAddress(value: String) { _recipientAddress.value = value }
    fun updateAmount(value: String) { _amount.value = value }
    fun updateDecimals(value: String) { _decimals.value = value }

    fun isFormValid(): Boolean {
        return _tokenMintAddress.value.isNotEmpty() &&
                _recipientAddress.value.isNotEmpty() &&
                _amount.value.isNotEmpty() &&
                _decimals.value.isNotEmpty() &&
                _amount.value.toDoubleOrNull() != null &&
                _amount.value.toDoubleOrNull()!! > 0 &&
                _decimals.value.toIntOrNull() != null
    }

    fun sendTokenTransfer() {
        val amountValue = _amount.value.replace(",", ".").toDoubleOrNull()
        if (amountValue == null || amountValue <= 0) {
            _errorMessage.value = "Invalid amount format"
            return
        }

        val decimalsValue = _decimals.value.toIntOrNull()
        if (decimalsValue == null || decimalsValue < 0 || decimalsValue > 18) {
            _errorMessage.value = "Invalid decimals (0-18)"
            return
        }

        if (_tokenMintAddress.value.isEmpty() || _recipientAddress.value.isEmpty()) {
            _errorMessage.value = "All fields are required"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _transactionSignature.value = null

            try {
                // Use devnet connection (configurable)
                val connection = Connection(Cluster.DEVNET)

                // Parse addresses
                val ownerPubkey = PublicKey(wallet.address)
                val mintPubkey = PublicKey(_tokenMintAddress.value)
                val recipientPubkey = PublicKey(_recipientAddress.value)

                val amountBigDecimal = BigDecimal(_amount.value.replace(",", "."))
                val multiplier = BigDecimal.TEN.pow(decimalsValue)
                val rawAmount = amountBigDecimal.multiply(multiplier).toLong()

                // Get associated token accounts
                val sourceTokenAccount = TokenProgram.getAssociatedTokenAddress(
                    mint = mintPubkey,
                    owner = ownerPubkey
                )



                val destinationTokenAccount = TokenProgram.getAssociatedTokenAddress(
                    mint = mintPubkey,
                    owner = recipientPubkey
                )

                // Check if destination token account exists
                val instructions = mutableListOf<TransactionInstruction>()

                // If destination doesn't exist, create it first
                val destExists = connection.accountExists(destinationTokenAccount)
                if (!destExists) {
                    val createAtaInstruction = TokenProgram.createAssociatedTokenAccountInstruction(
                        payer = ownerPubkey,
                        associatedToken = destinationTokenAccount,
                        owner = recipientPubkey,
                        mint = mintPubkey
                    )
                    instructions.add(createAtaInstruction)
                }

                // Add transfer instruction
                val transferInstruction = TokenProgram.transferChecked(
                    source = sourceTokenAccount,
                    mint = mintPubkey,
                    destination = destinationTokenAccount,
                    owner = ownerPubkey,
                    amount = rawAmount,
                    decimals = decimalsValue
                )
                instructions.add(transferInstruction)

                // Get latest blockhash
                val blockhash = connection.getLatestBlockhash()

                // Create v0 transaction
                val transaction = Transaction.v0(
                    payer = ownerPubkey,
                    instructions = instructions,
                    recentBlockhash = blockhash.blockhash
                )

                // Serialize full unsigned transaction to base64
                val base64 = transaction.serializeUnsignedToBase64()

                // Send transaction through Dynamic SDK
                val signature = sdk.solana.signAndSendTransaction(base64, wallet)
                _transactionSignature.value = signature

            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to send token transfer"
                e.printStackTrace()
            }

            _isLoading.value = false
        }
    }


}

class SolanaSendTokenViewModelFactory(private val wallet: BaseWallet) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SolanaSendTokenViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SolanaSendTokenViewModel(wallet) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
