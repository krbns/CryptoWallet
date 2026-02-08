package com.dynamic.sdk.example.feature.send.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dynamic.sdk.example.core.ui.components.ErrorMessageView
import com.dynamic.sdk.example.core.ui.components.InfoCard
import com.dynamic.sdk.example.core.ui.components.PrimaryButton
import com.dynamic.sdk.example.core.ui.components.SuccessMessageView
import com.dynamic.sdk.example.core.ui.theme.DynamicSDKExampleTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendTransactionScreen(
    onNavigateBack: () -> Unit,
    viewModel: SendTransactionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isLoading = uiState.status is SendTxStatus.Loading

    Scaffold(
        topBar = {
            SendTransactionTopBar(onNavigateBack = onNavigateBack)
        }
    ) { paddingValues ->
        SendTransactionContent(
            uiState = uiState,
            isLoading = isLoading,
            isFormValid = viewModel.isFormValid(),
            onRecipientChanged = viewModel::updateRecipientAddress,
            onAmountChanged = viewModel::updateAmountEth,
            onSendClick = viewModel::sendTransaction,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SendTransactionTopBar(onNavigateBack: () -> Unit) {
    TopAppBar(
        title = { Text("Send Transaction") },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        }
    )
}

@Composable
private fun SendTransactionContent(
    uiState: SendTransactionUiState,
    isLoading: Boolean,
    isFormValid: Boolean,
    onRecipientChanged: (String) -> Unit,
    onAmountChanged: (String) -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "From: ${uiState.walletAddress}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = uiState.recipientAddress,
            onValueChange = onRecipientChanged,
            label = { Text("Recipient Address") },
            placeholder = { Text("0x...") },
            singleLine = true,
            isError = uiState.recipientError != null
        )
        if (uiState.recipientError != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = uiState.recipientError!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = uiState.amountEth,
            onValueChange = onAmountChanged,
            label = { Text("Amount (ETH)") },
            placeholder = { Text("0.001") },
            singleLine = true,
            isError = uiState.amountError != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        if (uiState.amountError != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = uiState.amountError!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        PrimaryButton(
            title = "Send Transaction",
            onClick = onSendClick,
            isLoading = isLoading,
            isDisabled = isLoading || !isFormValid
        )

        Spacer(modifier = Modifier.height(20.dp))
        when (val status = uiState.status) {
            is SendTxStatus.Idle -> Unit
            is SendTxStatus.Loading -> Text(
                text = "Sending transaction...",
                style = MaterialTheme.typography.bodyMedium
            )
            is SendTxStatus.Success -> {
                SuccessMessageView(message = "Transaction sent successfully")
                Spacer(modifier = Modifier.height(10.dp))
                InfoCard(title = "Transaction Hash", content = status.txHash)
            }
            is SendTxStatus.Error -> ErrorMessageView(message = status.message)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SendTransactionScreenPreview() {
    DynamicSDKExampleTheme {
        Scaffold(
            topBar = { SendTransactionTopBar(onNavigateBack = {}) }
        ) { paddingValues ->
            SendTransactionContent(
                uiState = SendTransactionUiState(
                    walletAddress = "0x742d35Cc6634C0532925a3b844Bc9e7595f0bDd7",
                    recipientAddress = "0x000000000000000000000000000000000000dead",
                    amountEth = "0.001",
                    status = SendTxStatus.Success("0xabc123...")
                ),
                isLoading = false,
                isFormValid = true,
                onRecipientChanged = {},
                onAmountChanged = {},
                onSendClick = {},
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}
