package com.example.ui.common

import java.util.Locale

/**
 * Credit balance as shown in the UI: two decimals for round cent amounts, four
 * when per-second billing has left a fractional remainder. Always formatted
 * with [Locale.US] because the value is paired with a literal `$` sign.
 */
fun formatBalance(balance: Double): String {
    val isWholeCents = balance * 100 == (balance * 100).toLong().toDouble()
    return String.format(Locale.US, if (isWholeCents) "%.2f" else "%.4f", balance)
}
