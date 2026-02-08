package com.dynamic.sdk.example.core.domain

import java.math.BigDecimal

private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
private val OTP_REGEX = Regex("^\\d{6}$")
private val EVM_ADDRESS_REGEX = Regex("^0x[a-fA-F0-9]{40}$")

fun isValidEmail(value: String): Boolean = EMAIL_REGEX.matches(value.trim())

fun isValidOtp(value: String): Boolean = OTP_REGEX.matches(value.trim())

fun isValidEvmAddress(value: String): Boolean = EVM_ADDRESS_REGEX.matches(value.trim())

fun isValidEthAmount(value: String): Boolean {
    val trimmed = value.trim()
    val parsed = trimmed.toBigDecimalOrNull() ?: return false
    if (parsed <= BigDecimal.ZERO) return false
    return parsed.scale() <= 18
}
