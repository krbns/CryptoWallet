package com.dynamic.sdk.example.core.data.repository

import com.dynamic.sdk.android.Models.BaseWallet
import com.dynamic.sdk.android.Models.Network
import com.dynamic.sdk.example.core.data.sdk.DynamicSdkProvider
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import javax.inject.Inject
import javax.inject.Singleton

interface WalletRepository {
    fun getPrimaryEvmWallet(): BaseWallet?
    fun getEvmWalletByAddress(address: String): BaseWallet?
    suspend fun getNetworkChainId(wallet: BaseWallet): Int?
    suspend fun switchToSepolia(wallet: BaseWallet)
    suspend fun getBalance(wallet: BaseWallet): String
}

@Singleton
class WalletRepositoryImpl @Inject constructor(
    private val sdkProvider: DynamicSdkProvider,
) : WalletRepository {
    override fun getPrimaryEvmWallet(): BaseWallet? {
        val sdk = sdkProvider.get()
        val primary = runCatching { sdk.wallets.primary }.getOrNull()
        if (primary != null && isEvmWallet(primary)) {
            return primary
        }
        return sdk.wallets.userWallets.firstOrNull(::isEvmWallet)
    }

    override fun getEvmWalletByAddress(address: String): BaseWallet? {
        return sdkProvider.get().wallets.userWallets.firstOrNull {
            it.address.equals(address, ignoreCase = true) && isEvmWallet(it)
        }
    }

    override suspend fun getNetworkChainId(wallet: BaseWallet): Int? {
        val network = sdkProvider.get().wallets.getNetwork(wallet)
        val jsonValue = network.value
        return if (jsonValue is JsonPrimitive) {
            jsonValue.intOrNull ?: jsonValue.contentOrNull?.toIntOrNull()
        } else {
            jsonValue.toString().toIntOrNull()
        }
    }

    override suspend fun switchToSepolia(wallet: BaseWallet) {
        sdkProvider.get().wallets.switchNetwork(wallet, Network.evm(SEPOLIA_CHAIN_ID))
    }

    override suspend fun getBalance(wallet: BaseWallet): String {
        return sdkProvider.get().wallets.getBalance(wallet)
    }

    companion object {
        const val SEPOLIA_CHAIN_ID = 11155111
    }

    private fun isEvmWallet(wallet: BaseWallet): Boolean {
        return wallet.chain.equals("EVM", ignoreCase = true)
    }
}
