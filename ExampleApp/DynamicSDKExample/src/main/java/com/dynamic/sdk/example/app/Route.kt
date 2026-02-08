package com.dynamic.sdk.example.app

sealed class Route(val route: String) {
    data object Login : Route("login")
    data object Wallet : Route("wallet")
    data object SendTransaction : Route("send/{address}") {
        fun create(address: String): String = "send/$address"
    }
}
