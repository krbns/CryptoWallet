package com.dynamic.sdk.example.feature.walletdetails.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dynamic.sdk.example.core.ui.components.ErrorMessageView
import com.dynamic.sdk.example.core.ui.components.PrimaryButton
import com.dynamic.sdk.example.core.ui.components.SecondaryButton
import com.dynamic.sdk.example.core.ui.theme.DynamicSDKExampleTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    onNavigateToSendTransaction: (String) -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: WalletViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadIfNeeded()
    }

    LaunchedEffect(Unit) {
        viewModel.navigationToLogin.collect { onNavigateToLogin() }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            WalletTopBar(
                onRefresh = { viewModel.refresh(isManual = true) },
                onLogout = viewModel::logout
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        WalletScreenContent(
            uiState = uiState,
            onRefresh = { viewModel.refresh(isManual = true) },
            onCopyAddress = {
                val address = uiState.walletAddress ?: return@WalletScreenContent
                copyToClipboard(context, address)
                coroutineScope.launch { snackbarHostState.showSnackbar("Address copied") }
            },
            onSendTransaction = {
                val address = uiState.walletAddress ?: return@WalletScreenContent
                onNavigateToSendTransaction(address)
            },
            onLogout = viewModel::logout,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WalletTopBar(
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
) {
    TopAppBar(
        title = { Text("Wallet Details") },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        actions = {
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
            IconButton(onClick = onLogout) {
                Icon(Icons.Default.Logout, contentDescription = "Logout")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WalletScreenContent(
    uiState: WalletUiState,
    onRefresh: () -> Unit,
    onCopyAddress: () -> Unit,
    onSendTransaction: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        if (uiState.isLoadingInitial) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(horizontal = 20.dp))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                WalletInfoCard(
                    title = "Address",
                    value = uiState.walletAddress ?: "-"
                )
                Spacer(modifier = Modifier.height(12.dp))
                WalletInfoCard(
                    title = "Network",
                    value = uiState.networkLabel ?: "-"
                )
                Spacer(modifier = Modifier.height(12.dp))
                WalletInfoCard(
                    title = "Balance",
                    value = uiState.balanceLabel ?: "-"
                )

                if (!uiState.errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    ErrorMessageView(message = uiState.errorMessage!!)
                }

                Spacer(modifier = Modifier.height(16.dp))
                PrimaryButton(
                    title = "Copy Address",
                    onClick = onCopyAddress,
                    isDisabled = uiState.walletAddress.isNullOrBlank(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                PrimaryButton(
                    title = "Send Transaction",
                    onClick = onSendTransaction,
                    isDisabled = uiState.walletAddress.isNullOrBlank()
                )

                Spacer(modifier = Modifier.height(12.dp))
                PrimaryButton(
                    title = "Logout",
                    onClick = onLogout,
                )
            }
        }
    }
}

@Composable
private fun WalletInfoCard(
    title: String,
    value: String,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun copyToClipboard(context: Context, value: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("wallet_address", value))
}

@Preview(showBackground = true)
@Composable
private fun WalletScreenPreview() {
    DynamicSDKExampleTheme {
        Scaffold(
            topBar = { WalletTopBar(onRefresh = {}, onLogout = {}) }
        ) { paddingValues ->
            WalletScreenContent(
                uiState = WalletUiState(
                    walletAddress = "0x742d35Cc6634C0532925a3b844Bc9e7595f0bDd7",
                    networkLabel = "Sepolia (11155111)",
                    balanceLabel = "0.045 ETH",
                    isLoadingInitial = false,
                    isRefreshing = false
                ),
                onRefresh = {},
                onCopyAddress = {},
                onSendTransaction = {},
                onLogout = {},
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}
