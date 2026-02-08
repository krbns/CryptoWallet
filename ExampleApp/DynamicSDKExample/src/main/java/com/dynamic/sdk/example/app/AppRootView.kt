package com.dynamic.sdk.example.app

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dynamic.sdk.example.feature.login.ui.LoginScreen
import com.dynamic.sdk.example.feature.send.ui.SendTransactionScreen
import com.dynamic.sdk.example.feature.walletdetails.ui.WalletScreen

@Composable
fun AppRootView() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Route.Login.route
    ) {
        composable(Route.Login.route) {
            LoginScreen(
                onNavigateToWallet = {
                    navController.navigate(Route.Wallet.route) {
                        popUpTo(Route.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Route.Wallet.route) {
            WalletScreen(
                onNavigateToSendTransaction = { walletAddress ->
                    navController.navigate(Route.SendTransaction.create(walletAddress))
                },
                onNavigateToLogin = {
                    navController.navigate(Route.Login.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Route.SendTransaction.route,
            arguments = listOf(navArgument("address") { type = NavType.StringType })
        ) {
            SendTransactionScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
