package com.dynamic.sdk.example.core.data.repository

import com.dynamic.sdk.example.core.data.sdk.DynamicSdkProvider
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface AuthRepository {
    fun observeTokenChanges(): Flow<String?>
    fun currentToken(): String?
    suspend fun sendEmailOtp(email: String)
    suspend fun verifyEmailOtp(code: String)
    suspend fun resendEmailOtp()
    suspend fun logout()
}

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val sdkProvider: DynamicSdkProvider,
) : AuthRepository {
    override fun observeTokenChanges(): Flow<String?> = sdkProvider.get().auth.tokenChanges

    override fun currentToken(): String? = sdkProvider.get().auth.token

    override suspend fun sendEmailOtp(email: String) {
        sdkProvider.get().auth.email.sendOTP(email)
    }

    override suspend fun verifyEmailOtp(code: String) {
        sdkProvider.get().auth.email.verifyOTP(code)
    }

    override suspend fun resendEmailOtp() {
        sdkProvider.get().auth.email.resendOTP()
    }

    override suspend fun logout() {
        sdkProvider.get().auth.logout()
    }
}
