package com.dynamic.sdk.example.Screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dynamic.sdk.android.DynamicSDK
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * SplashScreenViewModel - handles initial app launch.
 * Listens to sdk.readyChanges, then checks token to decide navigation.
 */
class SplashScreenViewModel : ViewModel() {
    private var didRoute = false

    fun start(
        onNavigateToLogin: () -> Unit,
        onNavigateToHome: () -> Unit
    ) {
        if (didRoute) return

        val sdk = DynamicSDK.getInstance()

        viewModelScope.launch {
            // Wait for SDK to be ready
            sdk.sdk.readyChanges.first { it == true }

            if (didRoute) return@launch
            didRoute = true

            // Check token
            val token = sdk.auth.token ?: ""

            if (token.isNotEmpty()) {
                android.util.Log.d("SplashVM", "✅ SDK ready + token exists -> Home")
                onNavigateToHome()
            } else {
                android.util.Log.d("SplashVM", "🔐 SDK ready + no token -> Login")
                onNavigateToLogin()
            }
        }
    }
}
