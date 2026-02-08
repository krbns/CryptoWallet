package com.dynamic.sdk.example.core.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dynamic.sdk.android.DynamicSDK
import com.dynamic.sdk.android.Models.MfaAddDevice
import com.dynamic.sdk.example.core.ui.components.CardContainer
import com.dynamic.sdk.example.core.ui.components.TextFieldWithLabel
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URLEncoder

enum class MfaAddDeviceStep {
    SETUP, VERIFY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MfaAddDeviceScreen(
    onNavigateBack: () -> Unit,
    onFinished: () -> Unit
) {
    val viewModel: com.dynamic.sdk.example.core.ui.screens.MfaAddDeviceViewModel = viewModel()
    val step by viewModel.step.collectAsState()
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    val code by viewModel.code.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val alertTitle by viewModel.alertTitle.collectAsState()
    val alertMessage by viewModel.alertMessage.collectAsState()

    val context = LocalContext.current

    // Alert Dialog
    if (alertTitle != null) {
        AlertDialog(
            onDismissRequest = {
                if (alertTitle == "Success") {
                    viewModel.dismissAlert()
                    onFinished()
                } else {
                    viewModel.dismissAlert()
                }
            },
            title = { Text(alertTitle!!) },
            text = { Text(alertMessage ?: "") },
            confirmButton = {
                TextButton(onClick = {
                    if (alertTitle == "Success") {
                        viewModel.dismissAlert()
                        onFinished()
                    } else {
                        viewModel.dismissAlert()
                    }
                }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Add MFA Device",
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
            when (step) {
                MfaAddDeviceStep.SETUP -> {
                    SetupStepContent(
                        isLoading = isLoading,
                        onGenerateSecret = { viewModel.generateSecret() }
                    )
                }
                MfaAddDeviceStep.VERIFY -> {
                    VerifyStepContent(
                        deviceInfo = deviceInfo,
                        code = code,
                        isLoading = isLoading,
                        onCodeChange = { viewModel.updateCode(it) },
                        onVerify = { viewModel.verifyDevice() },
                        onCopySecret = {
                            deviceInfo?.secret?.let { secret ->
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("secret", secret))
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SetupStepContent(
    isLoading: Boolean,
    onGenerateSecret: () -> Unit
) {
    CardContainer {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Set up Authenticator App",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "You'll need an authenticator app like Google Authenticator, Authy, or 1Password to generate verification codes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Steps:",
                fontWeight = FontWeight.Bold
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "1. Install an authenticator app on your device",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "2. Copy the secret code",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "3. Enter the secret code in your authenticator app",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "4. Enter the verification code to complete setup",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onGenerateSecret,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Generate Secret")
                }
            }
        }
    }
}

@Composable
fun VerifyStepContent(
    deviceInfo: MfaAddDevice?,
    code: String,
    isLoading: Boolean,
    onCodeChange: (String) -> Unit,
    onVerify: () -> Unit,
    onCopySecret: () -> Unit
) {
    CardContainer {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Scan QR Code or Copy Secret",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // QR Code
            deviceInfo?.let { info ->
                val qrBitmap = remember(info.secret) {
                    generateQRCode(info.secret)
                }

                qrBitmap?.let { bitmap ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "QR Code",
                                modifier = Modifier
                                    .size(200.dp)
                                    .padding(12.dp)
                                    .background(Color.White)
                            )
                        }
                    }
                }
            }

            Divider()

            Text(
                text = "Or copy the secret code manually:",
                fontWeight = FontWeight.Medium
            )

            deviceInfo?.secret?.let { secret ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = secret,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    OutlinedButton(
                        onClick = onCopySecret,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Copy Secret")
                    }
                }
            }

            Divider()

            TextFieldWithLabel(
                label = "Verification Code",
                placeholder = "Enter 6-digit code from your authenticator app",
                value = code,
                onValueChange = onCodeChange,
                keyboardType = KeyboardType.Number
            )

            Button(
                onClick = onVerify,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && code.trim().isNotEmpty()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Verify & Add Device")
                }
            }
        }
    }
}

private fun generateQRCode(secret: String): Bitmap? {
    return try {
        val sdk = DynamicSDK.getInstance()
        val user = sdk.auth.authenticatedUser
        val accountName = user?.email ?: user?.userId ?: "Account"
        val issuer = sdk.props.appName ?: "Dynamic"

        val encodedIssuer = URLEncoder.encode(issuer, "UTF-8")
        val encodedAccount = URLEncoder.encode(accountName, "UTF-8")
        val totpUri = "otpauth://totp/$encodedIssuer:$encodedAccount?secret=$secret&issuer=$encodedIssuer"

        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(totpUri, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}

class MfaAddDeviceViewModel : ViewModel() {
    private val sdk = DynamicSDK.getInstance()

    private val _step = MutableStateFlow(MfaAddDeviceStep.SETUP)
    val step: StateFlow<MfaAddDeviceStep> = _step.asStateFlow()

    private val _deviceInfo = MutableStateFlow<MfaAddDevice?>(null)
    val deviceInfo: StateFlow<MfaAddDevice?> = _deviceInfo.asStateFlow()

    private val _code = MutableStateFlow("")
    val code: StateFlow<String> = _code.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _alertTitle = MutableStateFlow<String?>(null)
    val alertTitle: StateFlow<String?> = _alertTitle.asStateFlow()

    private val _alertMessage = MutableStateFlow<String?>(null)
    val alertMessage: StateFlow<String?> = _alertMessage.asStateFlow()

    fun updateCode(value: String) {
        _code.value = value
    }

    fun generateSecret() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val device = sdk.mfa.addDevice("totp")
                _deviceInfo.value = device
                _step.value = MfaAddDeviceStep.VERIFY
            } catch (e: Exception) {
                _alertTitle.value = "Error"
                _alertMessage.value = "Failed to add MFA device: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    fun verifyDevice() {
        if (_deviceInfo.value == null) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                sdk.mfa.verifyDevice(_code.value.trim(), "totp")
                _alertTitle.value = "Success"
                _alertMessage.value = "MFA device added successfully"
            } catch (e: Exception) {
                _alertTitle.value = "Error"
                _alertMessage.value = "Failed to verify MFA device. Please check your code and try again."
            }
            _isLoading.value = false
        }
    }

    fun dismissAlert() {
        _alertTitle.value = null
        _alertMessage.value = null
    }
}
