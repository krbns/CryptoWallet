package com.dynamic.sdk.example.core.data.sdk

import com.dynamic.sdk.android.DynamicSDK
import javax.inject.Inject
import javax.inject.Singleton

interface DynamicSdkProvider {
    fun get(): DynamicSDK
}

@Singleton
class DynamicSdkProviderImpl @Inject constructor() : DynamicSdkProvider {
    override fun get(): DynamicSDK = DynamicSDK.getInstance()
}
