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
import com.dynamic.sdk.example.Components.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignMessageScreen(
    onNavigateBack: () -> Unit,
    wallet: BaseWallet
) {
    val viewModel: SignMessageViewModel = viewModel(
        factory = SignMessageViewModelFactory(wallet)
    )
    val message by viewModel.message.collectAsState()
    val signedMessage by viewModel.signedMessage.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Sign Message",
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
            title = "Sign Message",
            content = "Sign a message with your wallet. This is a common way to prove ownership of a wallet address.",
            copyable = false
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Message Input
        TextFieldWithLabel(
            label = "Message",
            placeholder = "Enter message to sign",
            value = message,
            onValueChange = { viewModel.updateMessage(it) }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Sign Button
        PrimaryButton(
            title = if (isLoading) "Signing..." else "Sign Message",
            onClick = { viewModel.signMessage() },
            isLoading = isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Error Message
        errorMessage?.let { error ->
            ErrorMessageView(message = error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Success Message
        signedMessage?.let { signed ->
            SuccessMessageView(message = signed)
        }
        }
    }
}

class SignMessageViewModel(private val wallet: BaseWallet) : ViewModel() {
    private val sdk = DynamicSDK.getInstance()

    private val _message = MutableStateFlow("Hello from Dynamic SDK!")
    val message: StateFlow<String> = _message.asStateFlow()

    private val _signedMessage = MutableStateFlow<String?>(null)
    val signedMessage: StateFlow<String?> = _signedMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun updateMessage(newMessage: String) {
        _message.value = newMessage
    }

    fun signMessage() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _signedMessage.value = null

            try {
                val signed = sdk.wallets.signMessage(wallet, _message.value)
                _signedMessage.value = signed
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to sign message"
            }

            _isLoading.value = false
        }
    }
}

class SignMessageViewModelFactory(private val wallet: BaseWallet) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SignMessageViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SignMessageViewModel(wallet) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
