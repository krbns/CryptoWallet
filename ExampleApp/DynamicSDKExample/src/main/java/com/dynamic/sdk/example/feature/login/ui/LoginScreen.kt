package com.dynamic.sdk.example.feature.login.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dynamic.sdk.example.core.ui.components.ErrorMessageView
import com.dynamic.sdk.example.core.ui.components.OtpVerificationSheet
import com.dynamic.sdk.example.core.ui.components.PrimaryButton
import com.dynamic.sdk.example.core.ui.theme.DynamicSDKExampleTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateToWallet: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navigation.collect { onNavigateToWallet() }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Crypto Wallet Test") })
        }
    ) { paddingValues ->
        LoginScreenContent(
            uiState = uiState,
            onEmailChange = viewModel::updateEmail,
            onSendOtp = viewModel::sendOtp,
            modifier = Modifier.padding(paddingValues)
        )
    }

    if (uiState.isOtpSheetVisible) {
        OtpVerificationSheet(
            title = "Email verification",
            subtitle = "We sent a code to ${uiState.email.trim()}",
            onVerify = viewModel::verifyOtp,
            onResend = viewModel::resendOtp,
            onDismiss = viewModel::dismissOtpSheet
        )
    }
}

@Composable
private fun LoginScreenContent(
    uiState: LoginUiState,
    onEmailChange: (String) -> Unit,
    onSendOtp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(36.dp))
        Text(
            text = "Login",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Sign in with Email OTP",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = uiState.email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            placeholder = { Text("name@example.com") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))
        PrimaryButton(
            title = "Send OTP",
            onClick = onSendOtp,
            isLoading = uiState.isSendingOtp,
            isDisabled = uiState.email.isBlank()
        )

        if (!uiState.errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            ErrorMessageView(message = uiState.errorMessage!!)
        }
    }
}

@Preview(showBackground = true)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginScreenPreview() {
    DynamicSDKExampleTheme {
        Scaffold(topBar = { TopAppBar(title = { Text("Crypto Wallet Test") }) }) { paddingValues ->
            LoginScreenContent(
                uiState = LoginUiState(email = "test@gmail.com"),
                onEmailChange = {},
                onSendOtp = {},
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}
