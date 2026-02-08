package com.dynamic.sdk.example.core.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dynamic.sdk.android.DynamicSDK
import com.dynamic.sdk.android.Models.UserPasskey
import com.dynamic.sdk.android.Models.DeletePasskeyRequest
import com.dynamic.sdk.android.Models.MfaCreateToken
import com.dynamic.sdk.example.core.ui.components.CardContainer
import com.dynamic.sdk.example.core.ui.components.ErrorMessageView
import com.dynamic.sdk.example.core.ui.components.SimpleButton
import com.dynamic.sdk.example.core.ui.theme.ErrorRed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasskeysScreen(
    onNavigateBack: () -> Unit
) {
    val viewModel: PasskeysViewModel = viewModel()
    val passkeys by viewModel.passkeys.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val alertTitle by viewModel.alertTitle.collectAsState()
    val alertMessage by viewModel.alertMessage.collectAsState()
    val showDeleteConfirm by viewModel.showDeleteConfirm.collectAsState()
    val deleteConfirmMessage by viewModel.deleteConfirmMessage.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadPasskeys()
    }

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

    // Delete Confirmation Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            title = { Text("Delete Passkey") },
            text = { Text(deleteConfirmMessage) },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteConfirmed() },
                    colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDelete() }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Passkeys",
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
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // Register Passkey Button
            SimpleButton(
                icon = Icons.Default.Add,
                title = "Register Passkey",
                onClick = { viewModel.registerPasskey() }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Passkeys State Card
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                errorMessage != null -> {
                    ErrorMessageView(
                        message = errorMessage!!,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    SimpleButton(
                        icon = Icons.Default.Refresh,
                        title = "Retry",
                        onClick = { viewModel.loadPasskeys() }
                    )
                }
                passkeys.isNullOrEmpty() -> {
                    CardContainer {
                        Column {
                            Text(
                                text = "No passkeys configured",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Register a passkey to enhance your account security",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        passkeys!!.forEach { passkey ->
                            PasskeyCard(
                                passkey = passkey,
                                onAuthenticateMFA = { viewModel.authenticateMfa() },
                                onDelete = { viewModel.confirmDelete(passkey) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun PasskeyCard(
    passkey: UserPasskey,
    onAuthenticateMFA: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = passkey.id,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "ID: ${passkey.id.take(8)}...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Created: ${formatDate(passkey.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            passkey.lastUsedAt?.let { lastUsed ->
                Text(
                    text = "Last used: ${formatDate(lastUsed)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (passkey.isDefault == true) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Default",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            FilledTonalButton(
                onClick = onAuthenticateMFA,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Authenticate MFA", fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ErrorRed,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Delete", fontWeight = FontWeight.Medium)
            }
        }
    }
}

private fun formatDate(dateString: String): String {
    // Date comes as ISO string, just show the date part
    return dateString.take(10)
}

class PasskeysViewModel : ViewModel() {
    private val sdk = DynamicSDK.getInstance()

    private val _passkeys = MutableStateFlow<List<UserPasskey>?>(null)
    val passkeys: StateFlow<List<UserPasskey>?> = _passkeys.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _alertTitle = MutableStateFlow<String?>(null)
    val alertTitle: StateFlow<String?> = _alertTitle.asStateFlow()

    private val _alertMessage = MutableStateFlow<String?>(null)
    val alertMessage: StateFlow<String?> = _alertMessage.asStateFlow()

    private val _showDeleteConfirm = MutableStateFlow(false)
    val showDeleteConfirm: StateFlow<Boolean> = _showDeleteConfirm.asStateFlow()

    private val _deleteConfirmMessage = MutableStateFlow("")
    val deleteConfirmMessage: StateFlow<String> = _deleteConfirmMessage.asStateFlow()

    private var pendingDeletePasskey: UserPasskey? = null

    fun loadPasskeys() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                _passkeys.value = sdk.passkeys.getPasskeys()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load passkeys: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    fun registerPasskey() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                sdk.passkeys.registerPasskey()
                _alertTitle.value = "Success"
                _alertMessage.value = "Passkey registered successfully"
                loadPasskeys()
            } catch (e: Exception) {
                _alertTitle.value = "Error"
                _alertMessage.value = "Failed to register passkey: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    fun authenticateMfa() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = sdk.passkeys.authenticatePasskeyMFA(
                    createMfaToken = MfaCreateToken(singleUse = true),
                    relatedOriginRpId = null
                )
                _alertTitle.value = "Success"
                _alertMessage.value = "Passkey authenticated successfully\n\nToken: ${response.jwt ?: "nil"}"
            } catch (e: Exception) {
                _alertTitle.value = "Error"
                _alertMessage.value = "Failed to authenticate passkey: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    fun confirmDelete(passkey: UserPasskey) {
        pendingDeletePasskey = passkey
        _deleteConfirmMessage.value = "Are you sure you want to delete passkey \"${passkey.id}\"? This action cannot be undone."
        _showDeleteConfirm.value = true
    }

    fun deleteConfirmed() {
        val passkey = pendingDeletePasskey ?: return
        _showDeleteConfirm.value = false

        viewModelScope.launch {
            _isLoading.value = true
            try {
                sdk.passkeys.deletePasskey(DeletePasskeyRequest(passkeyId = passkey.id))
                loadPasskeys()
                _alertTitle.value = "Success"
                _alertMessage.value = "Passkey deleted successfully"
            } catch (e: Exception) {
                _alertTitle.value = "Error"
                _alertMessage.value = "Failed to delete passkey: ${e.message}"
            }
            _isLoading.value = false
            pendingDeletePasskey = null
        }
    }

    fun cancelDelete() {
        _showDeleteConfirm.value = false
        pendingDeletePasskey = null
    }

    fun dismissAlert() {
        _alertTitle.value = null
        _alertMessage.value = null
    }
}
