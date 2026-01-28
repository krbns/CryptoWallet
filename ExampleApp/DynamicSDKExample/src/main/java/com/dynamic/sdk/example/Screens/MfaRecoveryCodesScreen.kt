package com.dynamic.sdk.example.Screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dynamic.sdk.android.DynamicSDK
import com.dynamic.sdk.android.Models.MfaAuthenticateRecoveryCode
import com.dynamic.sdk.android.Models.MfaCreateToken
import com.dynamic.sdk.example.Components.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MfaRecoveryCodesScreen(
    onNavigateBack: () -> Unit
) {
    val viewModel: MfaRecoveryCodesViewModel = viewModel()
    val recoveryCode by viewModel.recoveryCode.collectAsState()
    val isPending by viewModel.isPending.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val alertTitle by viewModel.alertTitle.collectAsState()
    val alertMessage by viewModel.alertMessage.collectAsState()
    val showCodesSheet by viewModel.showCodesSheet.collectAsState()
    val codesSheetTitle by viewModel.codesSheetTitle.collectAsState()
    val codes by viewModel.codes.collectAsState()

    // Alert Dialog
    if (alertTitle != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAlert() },
            title = { Text(alertTitle!!) },
            text = { Text(alertMessage ?: "") },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissAlert() }) {
                    Text("OK")
                }
            }
        )
    }

    // Codes Sheet Dialog
    if (showCodesSheet) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissCodesSheet() },
            title = { Text(codesSheetTitle) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Save these codes in a safe place. Each code can only be used once.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    codes.forEach { code ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = code,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissCodesSheet() }) {
                    Text("Done")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Recovery Codes",
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
            // Authenticate Recovery Code Section
            CardContainer {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Authenticate Recovery Code",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Enter a recovery code to authenticate and receive an MFA token.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TextFieldWithLabel(
                    label = "Recovery Code",
                    placeholder = "Enter recovery code",
                    value = recoveryCode,
                    onValueChange = { viewModel.updateRecoveryCode(it) }
                )

                Button(
                    onClick = { viewModel.authenticateRecoveryCode() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading && recoveryCode.trim().isNotEmpty()
                ) {
                    Text(if (isLoading) "Loading..." else "Authenticate Recovery Code")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Divider()

        Spacer(modifier = Modifier.height(16.dp))

        // Get New Recovery Codes Section
        CardContainer {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Get New Recovery Codes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Generate new recovery codes. This will invalidate any existing recovery codes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = { viewModel.getNewRecoveryCodes() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    Text(if (isLoading) "Loading..." else "Get New Recovery Codes")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Divider()

        Spacer(modifier = Modifier.height(16.dp))

        // Check Pending Acknowledgment Section
        CardContainer {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Check Pending Acknowledgment",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Check if you have pending recovery codes that need to be acknowledged.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    onClick = { viewModel.checkPending() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    Text(if (isLoading) "Loading..." else "Check Pending")
                }

                isPending?.let { pending ->
                    Text(
                        text = if (pending) {
                            "You have pending recovery codes that need to be acknowledged"
                        } else {
                            "No pending recovery codes acknowledgment required"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Acknowledge Recovery Codes Section
        CardContainer {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Acknowledge Recovery Codes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Acknowledge your recovery codes so they can be used.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = { viewModel.acknowledge() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    Text(if (isLoading) "Loading..." else "Acknowledge")
                }
            }
        }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

class MfaRecoveryCodesViewModel : ViewModel() {
    private val sdk = DynamicSDK.getInstance()

    private val _recoveryCode = MutableStateFlow("")
    val recoveryCode: StateFlow<String> = _recoveryCode.asStateFlow()

    private val _isPending = MutableStateFlow<Boolean?>(null)
    val isPending: StateFlow<Boolean?> = _isPending.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _alertTitle = MutableStateFlow<String?>(null)
    val alertTitle: StateFlow<String?> = _alertTitle.asStateFlow()

    private val _alertMessage = MutableStateFlow<String?>(null)
    val alertMessage: StateFlow<String?> = _alertMessage.asStateFlow()

    private val _showCodesSheet = MutableStateFlow(false)
    val showCodesSheet: StateFlow<Boolean> = _showCodesSheet.asStateFlow()

    private val _codesSheetTitle = MutableStateFlow("")
    val codesSheetTitle: StateFlow<String> = _codesSheetTitle.asStateFlow()

    private val _codes = MutableStateFlow<List<String>>(emptyList())
    val codes: StateFlow<List<String>> = _codes.asStateFlow()

    fun updateRecoveryCode(value: String) {
        _recoveryCode.value = value
    }

    fun authenticateRecoveryCode() {
        if (_recoveryCode.value.trim().isEmpty()) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                sdk.mfa.authenticateRecoveryCode(
                    MfaAuthenticateRecoveryCode(
                        code = _recoveryCode.value.trim(),
                        createMfaToken = MfaCreateToken(singleUse = true)
                    )
                )
                _alertTitle.value = "Success"
                _alertMessage.value = "Recovery code authenticated successfully"
                _recoveryCode.value = ""
            } catch (e: Exception) {
                _alertTitle.value = "Error"
                _alertMessage.value = "Failed to authenticate recovery code. Please check your code and try again."
            }
            _isLoading.value = false
        }
    }

    fun getNewRecoveryCodes() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val newCodes = sdk.mfa.getRecoveryCodes(generateNewCodes = true)
                _codes.value = newCodes
                _codesSheetTitle.value = "Recovery Codes"
                _showCodesSheet.value = true
            } catch (e: Exception) {
                _alertTitle.value = "Error"
                _alertMessage.value = "Failed to get new recovery codes: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    fun checkPending() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _isPending.value = sdk.mfa.isPendingRecoveryCodesAcknowledgment()
            } catch (e: Exception) {
                _alertTitle.value = "Error"
                _alertMessage.value = "Failed to check pending acknowledgment: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    fun acknowledge() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                sdk.mfa.completeAcknowledgement()
                _alertTitle.value = "Success"
                _alertMessage.value = "Recovery codes acknowledged successfully"
            } catch (e: Exception) {
                _alertTitle.value = "Error"
                _alertMessage.value = "Failed to acknowledge recovery codes: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    fun dismissAlert() {
        _alertTitle.value = null
        _alertMessage.value = null
    }

    fun dismissCodesSheet() {
        _showCodesSheet.value = false
    }
}
