package com.dynamic.sdk.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.dynamic.sdk.android.DynamicSDK
import com.dynamic.sdk.android.UI.DynamicUI
import com.dynamic.sdk.android.core.ClientProps
import com.dynamic.sdk.android.core.LoggerLevel
import com.dynamic.sdk.example.App.AppRootView
import com.dynamic.sdk.example.ui.theme.DynamicSDKExampleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Dynamic SDK
        val props = ClientProps(
            environmentId = "3e219b76-dcf1-40ab-aad6-652c4dfab4cc",
            appLogoUrl = "https://demo.dynamic.xyz/favicon-32x32.png",
            appName = "Dynamic Android Demo",
            redirectUrl = "dynamicandroiddemo://",
            appOrigin = "https://demo.dynamic.xyz",
            logLevel = LoggerLevel.DEBUG,
        )
        DynamicSDK.initialize(props, applicationContext, this)

        setContent {
            DynamicSDKExampleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Main app content
                        AppRootView()

                        // Dynamic SDK WebView overlay (shows when auth/profile is opened)
                        DynamicUI()
                    }
                }
            }
        }
    }
}

