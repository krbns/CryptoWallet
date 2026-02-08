package com.dynamic.sdk.example.domain

import com.dynamic.sdk.example.core.domain.isValidEmail
import com.dynamic.sdk.example.core.domain.isValidEthAmount
import com.dynamic.sdk.example.core.domain.isValidEvmAddress
import com.dynamic.sdk.example.core.domain.isValidOtp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsTest {
    @Test
    fun `email validator accepts valid email`() {
        assertTrue(isValidEmail("user@example.com"))
    }

    @Test
    fun `email validator rejects invalid email`() {
        assertFalse(isValidEmail("userexample.com"))
    }

    @Test
    fun `otp validator accepts six digits`() {
        assertTrue(isValidOtp("123456"))
    }

    @Test
    fun `otp validator rejects non digit value`() {
        assertFalse(isValidOtp("12ab56"))
    }

    @Test
    fun `evm address validator accepts valid address`() {
        assertTrue(isValidEvmAddress("0x742d35Cc6634C0532925a3b844Bc9e7595f0bDd7"))
    }

    @Test
    fun `evm address validator rejects invalid address`() {
        assertFalse(isValidEvmAddress("0x123"))
    }

    @Test
    fun `amount validator accepts positive value`() {
        assertTrue(isValidEthAmount("0.001"))
    }

    @Test
    fun `amount validator rejects zero or negative value`() {
        assertFalse(isValidEthAmount("0"))
        assertFalse(isValidEthAmount("-1"))
    }
}
