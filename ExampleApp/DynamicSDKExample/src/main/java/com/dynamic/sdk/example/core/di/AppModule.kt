package com.dynamic.sdk.example.core.di

import com.dynamic.sdk.example.core.data.repository.AuthRepository
import com.dynamic.sdk.example.core.data.repository.AuthRepositoryImpl
import com.dynamic.sdk.example.core.data.repository.TransactionRepository
import com.dynamic.sdk.example.core.data.repository.TransactionRepositoryImpl
import com.dynamic.sdk.example.core.data.repository.WalletRepository
import com.dynamic.sdk.example.core.data.repository.WalletRepositoryImpl
import com.dynamic.sdk.example.core.data.sdk.DynamicSdkProvider
import com.dynamic.sdk.example.core.data.sdk.DynamicSdkProviderImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindingsModule {
    @Binds
    @Singleton
    abstract fun bindDynamicSdkProvider(impl: DynamicSdkProviderImpl): DynamicSdkProvider

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindWalletRepository(impl: WalletRepositoryImpl): WalletRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository
}

@Module
@InstallIn(SingletonComponent::class)
object AppDispatchersModule {
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
