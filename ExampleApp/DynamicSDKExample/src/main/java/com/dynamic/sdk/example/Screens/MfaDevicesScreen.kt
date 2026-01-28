package com.dynamic.sdk.example.Screens

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
import com.dynamic.sdk.android.Models.MfaDevice
import com.dynamic.sdk.android.Models.MfaDeviceType
import com.dynamic.sdk.android.Models.MfaAuthenticateDevice
import com.dynamic.sdk.android.Models.MfaCreateToken
import com.dynamic.sdk.android.core.Logger
import com.dynamic.sdk.example.Components.*
import com.dynamic.sdk.example.ui.theme.ErrorRed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MfaDevicesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddDevice: () -> Unit,
    onNavigateToRecoveryCodes: () -> Unit
) {
    val viewModel: MfaDevicesViewModel = viewModel()
    val devices by viewModel.devices.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val alertTitle by viewModel.alertTitle.collectAsState()
    val alertMessage by viewModel.alertMessage.collectAsState()
    val showCodeDialog by viewModel.showCodeDialog.collectAsState()
    val codeDialogTitle by viewModel.codeDialogTitle.collectAsState()
    val codeDialogMessage by viewModel.codeDialogMessage.collectAsState()

    var codeInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadDevices()
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

    // Code Input Dialog
    if (showCodeDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissCodeDialog() },
            title = { Text(codeDialogTitle) },
            text = {
                Column {
                    Text(codeDialogMessage)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = { codeInput = it },
                        label = { Text("TOTP Code") },
                        placeholder = { Text("Enter 6-digit code") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.handleCodeSubmit(codeInput)
                    codeInput = ""
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.dismissCodeDialog()
                    codeInput = ""
                }) {
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
                        "MFA Devices",
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
            // Add MFA Device Button
            SimpleButton(
                icon = Icons.Default.Add,
                title = "Add MFA Device",
                onClick = onNavigateToAddDevice
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Get New Recovery Codes Button
            SimpleButton(
                icon = Icons.Default.Refresh,
                title = "Get New Recovery Codes",
                onClick = onNavigateToRecoveryCodes
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Devices State Card
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
                        onClick = { viewModel.loadDevices() }
                    )
                }
                devices.isNullOrEmpty() -> {
                    CardContainer {
                        Column {
                            Text(
                                text = "No MFA devices configured",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Add an MFA device to enhance your account security",
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
                        devices!!.forEach { device ->
                            MfaDeviceCard(
                                device = device,
                                onAuthenticate = { viewModel.presentCodePrompt(device, MfaAction.AUTHENTICATE) },
                                onRegenerateBackupCodes = { viewModel.regenerateBackupCodes(device) },
                                onDelete = { viewModel.presentCodePrompt(device, MfaAction.DELETE) }
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
fun MfaDeviceCard(
    device: MfaDevice,
    onAuthenticate: () -> Unit,
    onRegenerateBackupCodes: () -> Unit,
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
                text = if (device.type == MfaDeviceType.totp) "Authenticator App" else (device.type?.name ?: "Unknown"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Device ID: ${device.id?.take(8) ?: "N/A"}...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            FilledTonalButton(
                onClick = onAuthenticate,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Authenticate device", fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(8.dp))

            FilledTonalButton(
                onClick = onRegenerateBackupCodes,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Regenerate backup code", fontWeight = FontWeight.Medium)
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

enum class MfaAction {
    AUTHENTICATE, DELETE
}

class MfaDevicesViewModel : ViewModel() {
    private val sdk = DynamicSDK.getInstance()

    private val _devices = MutableStateFlow<List<MfaDevice>?>(null)
    val devices: StateFlow<List<MfaDevice>?> = _devices.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _alertTitle = MutableStateFlow<String?>(null)
    val alertTitle: StateFlow<String?> = _alertTitle.asStateFlow()

    private val _alertMessage = MutableStateFlow<String?>(null)
    val alertMessage: StateFlow<String?> = _alertMessage.asStateFlow()

    private val _showCodeDialog = MutableStateFlow(false)
    val showCodeDialog: StateFlow<Boolean> = _showCodeDialog.asStateFlow()

    private val _codeDialogTitle = MutableStateFlow("")
    val codeDialogTitle: StateFlow<String> = _codeDialogTitle.asStateFlow()

    private val _codeDialogMessage = MutableStateFlow("")
    val codeDialogMessage: StateFlow<String> = _codeDialogMessage.asStateFlow()

    private var pendingDevice: MfaDevice? = null
    private var pendingAction: MfaAction? = null

    fun loadDevices() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                _devices.value = sdk.mfa.getUserDevices()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load MFA devices: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    fun presentCodePrompt(device: MfaDevice, action: MfaAction) {
        pendingDevice = device
        pendingAction = action
        _codeDialogTitle.value = "Authenticate device"
        _codeDialogMessage.value = when (action) {
            MfaAction.AUTHENTICATE -> "Enter the TOTP code from your authenticator app to authenticate this device"
            MfaAction.DELETE -> "Enter the TOTP code from your authenticator app to delete this MFA device"
        }
        _showCodeDialog.value = true
    }

    fun handleCodeSubmit(code: String) {
        _showCodeDialog.value = false
        val device = pendingDevice ?: return
        val action = pendingAction ?: return
        val trimmedCode = code.trim()
        if (trimmedCode.isEmpty()) return

        when (action) {
            MfaAction.AUTHENTICATE -> authenticateDevice(device, trimmedCode)
            MfaAction.DELETE -> deleteDevice(device, trimmedCode)
        }
    }

    private fun authenticateDevice(device: MfaDevice, code: String) {
        val deviceId = device.id ?: return
        viewModelScope.launch {
            try {
                val token = sdk.mfa.authenticateDevice(
                    MfaAuthenticateDevice(
                        code = code,
                        deviceId = deviceId,
                        createMfaToken = MfaCreateToken(singleUse = true)
                    )
                )
                _alertTitle.value = "Success"
                _alertMessage.value = "Device authenticated successfully\n\nMFA Token: ${token ?: "nil"}"
            } catch (e: Exception) {
                _alertTitle.value = "Error"
                _alertMessage.value = "Failed to authenticate device: ${e.message}"
            }
        }
    }

    fun regenerateBackupCodes(device: MfaDevice) {
        viewModelScope.launch {
            try {
                val codes = sdk.mfa.getRecoveryCodes(generateNewCodes = true)
                _alertTitle.value = "Backup Codes"
                _alertMessage.value = codes.joinToString("\n")
            } catch (e: Exception) {
                _alertTitle.value = "Error"
                _alertMessage.value = "Failed to get backup codes: ${e.message}"
            }
        }
    }

    private fun deleteDevice(device: MfaDevice, code: String) {
        val deviceId = device.id ?: return
        viewModelScope.launch {
            try {
                val token = sdk.mfa.authenticateDevice(
                    MfaAuthenticateDevice(
                        code = code,
                        deviceId = deviceId,
                        createMfaToken = MfaCreateToken(singleUse = true)
                    )
                )

                if (token.isNullOrEmpty()) {
                    _alertTitle.value = "Error"
                    _alertMessage.value = "Failed to authenticate device"
                    return@launch
                }

                sdk.mfa.deleteUserDevice(deviceId, token)
                _alertTitle.value = "Success"
                _alertMessage.value = "Device deleted successfully"
                loadDevices()

            } catch (e: Exception) {
                _alertTitle.value = "Error"
                _alertMessage.value = "Failed to delete MFA device: ${e.message}"
            }
        }
    }

    fun dismissAlert() {
        _alertTitle.value = null
        _alertMessage.value = null
    }

    fun dismissCodeDialog() {
        _showCodeDialog.value = false
        pendingDevice = null
        pendingAction = null
    }
}
