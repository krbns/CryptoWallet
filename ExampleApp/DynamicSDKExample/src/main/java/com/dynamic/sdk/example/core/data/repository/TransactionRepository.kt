package com.dynamic.sdk.example.core.data.repository

import com.dynamic.sdk.android.Chains.EVM.EthereumTransaction
import com.dynamic.sdk.android.Chains.EVM.convertEthToWei
import com.dynamic.sdk.android.Models.BaseWallet
import com.dynamic.sdk.example.core.data.sdk.DynamicSdkProvider
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

interface TransactionRepository {
    suspend fun sendSepoliaTransaction(
        wallet: BaseWallet,
        recipientAddress: String,
        amountEth: String,
    ): String
}

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val sdkProvider: DynamicSdkProvider,
) : TransactionRepository {
    override suspend fun sendSepoliaTransaction(
        wallet: BaseWallet,
        recipientAddress: String,
        amountEth: String,
    ): String {
        val sdk = sdkProvider.get()
        val client = sdk.evm.createPublicClient(SEPOLIA_CHAIN_ID)
        val gasPrice = client.getGasPrice()
        val maxFeePerGas = gasPrice * BigInteger.valueOf(2)
        val maxPriorityFeePerGas = gasPrice

        val transaction = EthereumTransaction(
            from = wallet.address,
            to = recipientAddress,
            value = convertEthToWei(amountEth),
            gas = BigInteger.valueOf(21000),
            maxFeePerGas = maxFeePerGas,
            maxPriorityFeePerGas = maxPriorityFeePerGas
        )

        return sdk.evm.sendTransaction(transaction, wallet)
    }

    companion object {
        const val SEPOLIA_CHAIN_ID = 11155111
    }
}
