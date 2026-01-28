package com.dynamic.sdk.example.Components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpVerificationSheet(
    title: String,
    subtitle: String?,
    onVerify: suspend (String) -> Unit,
    onResend: suspend () -> Unit,
    onDismiss: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var didVerify by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (subtitle != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // OTP Code input
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("OTP code") },
                placeholder = { Text("123456") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Error message
            if (errorMessage != null) {
                ErrorMessageView(message = errorMessage!!)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Success message
            if (didVerify) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        "✓ Verified!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Verify button
            PrimaryButton(
                title = if (isLoading) "Verifying..." else "Verify",
                onClick = {
                    val trimmed = code.trim()
                    if (trimmed.isEmpty()) return@PrimaryButton

                    scope.launch {
                        isLoading = true
                        errorMessage = null
                        didVerify = false
                        try {
                            onVerify(trimmed)
                            didVerify = true
                            // Auto-dismiss after successful verification
                            kotlinx.coroutines.delay(500)
                            onDismiss()
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "Verification failed"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                isLoading = isLoading,
                isDisabled = code.trim().isEmpty()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Resend button
            TextButton(
                onClick = {
                    scope.launch {
                        try {
                            errorMessage = null
                            onResend()
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "Failed to resend code"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text(
                    "Resend code",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
